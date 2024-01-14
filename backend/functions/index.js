const functions = require("firebase-functions");
const Parser = require('rss-parser');
var crypto = require('crypto');
let parser = new Parser({
   customFields: {
      item: [
         'guid',
         'media:thumbnail',
         'media:group',
         'media:content',
         ['media:content', 'media:content', {keepArray: true}],
      ],
   }
});
const admin = require('firebase-admin');
admin.initializeApp();

function getImage(item) {
   if (item["media:thumbnail"] !== undefined) {
      if (item["media:thumbnail"].$ !== undefined) {
         if (item["media:thumbnail"].$.url !== undefined) {
            return item["media:thumbnail"].$.url
         }
      }
   }
   if (item["media:group"] !== undefined) {
      if (item["media:group"]["media:content"] !== undefined) {
         let min = 1000
         for (const content of item["media:group"]["media:content"]) {
            let width = content.$.width
            if (width !== undefined) {
               if (width < min) {
                  min = width
                  return content.$.url
               }
            }
         }
      }
   }
   if (item["media:content"] !== undefined) {
      const content = item["media:content"]
      if (Array.isArray(content)) {
         for (const contentEntry of content) {
            if (contentEntry.$ !== undefined) {
               if (contentEntry.$.url !== undefined) {
                  return contentEntry.$.url
               }
            }
         }
      }
   }
   return ""
}

function getDescription(item) {
   if (item.contentSnippet === undefined) {
      item.contentSnippet = ""
   }
   return item.contentSnippet.substring(0, 200)
}

function getId(item) {
   if (item.id !== undefined && item.id != "") {
      return item.id
   }
   if (item.guid !== undefined && item.guid != "") {
      return crypto.createHash('md5').update(item.guid).digest('hex');
   }
   functions.logger.warn("No id or guid found in item: ", item)
}

function processFeedItem(item, previousUpdated) {
   if (item.pubDate === undefined) {
      functions.logger.warn("No pubDate found in item: ", item)
      return
   }
   let updated = new Date(item.pubDate)
   if (updated <= previousUpdated) {
      // This item has not changed since last time
      return
   }
   const description = getDescription(item)
   const id = getId(item)
   const image = getImage(item)
   return {
      id : id,
      data : {
         title: item.title,
         pubDate: updated,
         link: item.link,
         description: description,
         image: image,
         num_comments: 0,
         new: true
      }
   }
}

function processSource(sourceDoc, feed) {
   // Check whether new posts have been published since last update
   let previousUpdated = new Date(null)
   if (sourceDoc.data().updated !== undefined) {
      previousUpdated = sourceDoc.data().updated.toDate()
   }
   const updated = new Date(feed.lastBuildDate)
   if (updated <= previousUpdated) {
      // Nothing new to store
      return
   }
   let db = admin.firestore()
   let batch = db.batch()
   batch.update(sourceDoc.ref, {"updated": updated})
   let postsCollection = sourceDoc.ref.collection('posts')
   let count = 0
   // Process each of the items (news articles)
   feed.items.every(item => {
      const ret = processFeedItem(item, previousUpdated)
      if (ret !== undefined) {
         let ref = postsCollection.doc(ret.id)
         batch.set(ref, ret.data)
         // Limit the amount of items we can store in one go
         if (++count >= 50) {
            return false
         }
      }
      return true
   })
   functions.logger.info("Source updated with " + count + " new items")
   return batch.commit()
}

// Fetch feeds for new posts
exports.updateRSS = functions.pubsub.schedule('every 24 hours').onRun(async (context) => {
   let db = admin.firestore()
   // Retrieve all the sources (typically there is just one)
   let querySnapshot = await db.collection("sources").get()
   return Promise.all(querySnapshot.docs.map((doc) => {
      // Sources can be marked as inactive
      if (doc.data().active) {
         // Retrieve the source and then process it
         return parser.parseURL(doc.data().url).then(feed => {
            return processSource(doc, feed)
         })
      }
   }))
});

// Clean up old posts to speed up queries
exports.setPostsAsOld = functions.pubsub.schedule('every 24 hours').onRun(async (context) => {
   // For all the posts in all active sources, set them as old if they are
   // older than seven days
   let db = admin.firestore()
   let querySnapshot = await db.collection("sources").get()
   return Promise.all(querySnapshot.docs.map(async (doc) => {
      if (!doc.data().active) {
         return
      }
      var date = new Date()
      date.setDate(date.getDate() - 7)
      let posts = await doc.ref.collection("posts")
            .where("new", "==", true)
            .where("pubDate", "<=", date)
            .get()
      let batch = db.batch()
      posts.forEach((post) => {
         batch.update(post.ref,
            {"new": admin.firestore.FieldValue.delete()}
         )
      })
      return batch.commit()
   }))
})

function getDelta(change) {
   if (!change.before.exists && change.after.exists) {
      // Created
      return 1;
   } else if (change.before.exists && !change.after.exists) {
      // Deleted
      return -1
   }
   // Updated
   return 0;
}

async function sendNotification(sourceId, userId, title, body) {
   const payload = {
      notification: {
        title: title,
        body: body,
        click_action: `MyCommentsActivity`,
      }
    }
    const user = await admin.firestore().collection("sources").doc(sourceId).
                                         collection("users").doc(userId).get()
    if (!user.exists) {
      return
    }
    const tokens = user.data().tokens
    // Remove duplicates
    const uniqueTokens = [...new Set(tokens)]
    if (uniqueTokens.length > 0) {
      const result = await admin.messaging().sendToDevice(uniqueTokens, payload);
    }
}

async function sendReplyNotification(sourceId, userId, name, text) {
   const body = text ? (text.length <= 100 ? text : text.substring(0, 97) + '...') : ''
   return sendNotification(sourceId, userId, `New reply from ${name}`, body)
}

// This function is called every time a user clicks like on a comment
exports.updateCommentLikeCounter = functions.firestore
   .document("sources/{sourceId}/users/{userId}/likes/{commentId}")
   .onWrite((change, context) => {
      let delta = getDelta(change)
      if (delta == 0) {
         return Promise.resolve()
      }
      let data = change.before.exists ? change.before.data() : change.after.data()
      let db = admin.firestore()
      // Atomically increment the likes counter
      return db
         .collection("sources").doc(context.params.sourceId)
         .collection("posts").doc(data.post_id)
         .collection("comments").doc(context.params.commentId)
         .update({
            likes: admin.firestore.FieldValue.increment(delta)
         })
})

// This function is called every time a user writes or deletes a comment
exports.commentCounter = functions.firestore
   .document("sources/{sourceId}/posts/{postId}/comments/{commentId}")
   .onWrite((change, context) => {
   let delta = getDelta(change);
   // First deal with possible push notifications
   if (delta == 0) {
      // The likes counter of the comment has been updated
      const afterLikes = change.after.data().likes
      // Notify on 1, 10, 100, 1000, etc. likes
      const log = Number.isInteger(Math.log10(afterLikes))
      if (afterLikes > change.before.data().likes && log) {
         const userId = change.before.data().author_id
         const body = `Your comment has ${afterLikes} likes`
         return sendNotification(context.params.sourceId, userId, "You comment is popular!", body)
      }
      return Promise.resolve()
   }
   var promises = []
   if (delta == 1) {
      // New message. Notify the other user if this comment is a reply
      const data = change.after.data()
      const inReplyTo = data.in_reply_to
      if (inReplyTo !== undefined && inReplyTo != null) {
         const authorId = inReplyTo.author_id
         if (authorId !== undefined) {
            promises.push(sendReplyNotification(context.params.sourceId, authorId, data.author_name, data.text))
         }
      }
   }
   // Finally increment the comment counter of the news article
   let db = admin.firestore()
   promises.push(db
      .collection("sources").doc(context.params.sourceId)
      .collection("posts").doc(context.params.postId)
      .update({
         num_comments: admin.firestore.FieldValue.increment(delta)
      }))
   return Promise.all(promises)
})

// When a comment is deleted, we should update the parent comments
// if this was a reply
exports.deleteReplies = functions.firestore
   .document("sources/{sourceId}/posts/{postId}/comments/{commentId}")
   .onDelete(async (change, context) => {
   let db = admin.firestore()
   let querySnapshot = await db
      .collection("sources").doc(context.params.sourceId)
      .collection("posts").doc(context.params.postId)
      .collection("comments")
      .where("in_reply_to.comment_id", "==", context.params.commentId)
      .get()
   let batch = db.batch()
   querySnapshot.forEach((comment) => {
      batch.update(comment.ref,
         {"in_reply_to.text": "<Deleted comment>"}
      )
   })
   return batch.commit()
})
