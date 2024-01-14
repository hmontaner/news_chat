NEWS CHAT
==========================================
News Chat (also known as Forum App) is an Android app that allows users to chat about news articles.
You can configure the backend of this app to fetch news articles from the source of your choice,
just pick an RSS feed you like and follow this guide to set up the backend and app.

The rest of this guide has two main parts: first you can follow the 8 steps to set up the backend,
then you can read about the structure and design of the code.

You can try the app in https://play.google.com/store/apps/details?id=app.forum

1. Create Firebase Project
------------------------------------------
The backend for this app is included in this package and must be set up before you can use the app.
It is developed using Google's Firebase technology. The first step is to create a new Firebase project;
you can find all the info in https://firebase.google.com.

2. Create a Cloud Firestore database
------------------------------------------
The app stores its data (e.g. messages) in a Cloud Firestore database.
You can follow the steps to create it in https://firebase.google.com/docs/firestore/quickstart#create
(ignore the subsequent steps to configure the Android app as it is already done).
Then you need to set up the database Rules that regulate permissions
(e.g. who can delete a certain comment). For that, go to
Firebase Console > Firestore database > Rules, and copy the content of the
file `rules.txt` that you can find in this package.

3. Setup your RSS feed
------------------------------------------
Once your database is created, you need to manually add at least one RSS feed.
The backend will periodically fetch news articles from there and store them in the database.
Any standard RSS feed should work, for example
http://rss.cnn.com/rss/edition.rss or https://feeds.skynews.com/feeds/rss/world.xml.

Go to Firebase Console > Firestore Database, and in the Panel View create an initial collection named `sources`.
Then, inside this collection, create a document with random id and these fields:
active: true
url: <the_url_of_your_RSS_feed>

Write down the id of this document as you will have to use it in the app.
In the app code, file "FirestoreRepository.kt", in the Companion Object there is a variable
named SOURCE that has to be initialised to the id of the document you just created.

4. Enable Authentication
------------------------------------------
The app allows users to enter by just choosing a name.
For that, you need to allow anonymous loging in Firebase.
The app also allows users to login using a Google account.
To configure this methods of authentication, go to
Firebase Console > Authentication and add these Sign-in providers:
- Anonymous
- Google (optional)
Additionally, if you are enabling Google Login,
you have to set up your app's SHA-1 fingerprint on your Project Settings
(see the relevant step in https://firebase.google.com/docs/auth/android/google-signin).

5. Enable Push Notifications (optional)
------------------------------------------
This app sends push notifications when somebody replies or likes a user's comments.
To activate this feature, you have to enable Cloud Messaging from the Google Cloud Console:
APIs & Services > Enable Apis And Services, then search for Cloud Messaging and enable that API.

6. Cloud Functions
------------------------------------------
Cloud functions constitute the code of the backend.
It is supplied within this package in the directory `backend`.
It is a set of functions that are executed in the server when certain events happen,
for example when somebody writes a comment and the comment counter needs incrementing.
You have to upload them to Firebase by following this guide:
https://firebase.google.com/docs/functions/get-started.
The command you should use to finally upload your backend to production is:
`firebase deploy --only functions` from the `backend` directory.

Note that some of these Cloud Functions are Scheduled functions, that is,
functions that are executed periodically.
For example, the RSS feed of your choice will be retrieved periodically to update the list of news articles.
This Scheduled Functions require to upgrade your Firebase project to Blaze pricing plan,
although it is typically free as the usage of this app is under the free tier
until you reach a few thousands of users. You can follow the guide in here
https://firebase.google.com/docs/functions/schedule-functions.

7. Connect Firebase to your Android app
------------------------------------------
You need to do two main things to connect your backend to your app.
First register your app in the Firebase Console and then download the Firebase configuration file
(google-services.json) and add it to the root directory of your app project.
You can follow this guide: https://firebase.google.com/docs/android/setup#console

8. Change your Application Id
------------------------------------------
Before publishing in Google Play you need to change the Application Id of your app as it has to be unique.
For that, go to the app file `build.gradle` and change the value of `applicationId` to something of your choice.

Source Code Structure and Design
------------------------------------------
`LoginActivity` is the initial Activity of the app.
It allows users to set a name and, optionally, login using a Google account.
If users decide to not login now they can do later in the Settings Activity.
Logging in with a Google Account has the advantage of being able to retrieve your
account in another phone so that you don't lose your comments.

`PostsActivity` is the main Activity. It shows news articles.
There are two lists of news articles, one ordered chronologically and the other by number of comments.
Each of these two lists is implemented in `PostsFragment`
and each item in the lists is displayed in `PostsRecyclerViewAdapter`.

Once the user clicks on a news article, they navigate to `CommentsActivity`.
This activity shows the comments that other users have written about that news article.
It has a similar structure to `PostsActivity`.

`FirestoreRepository` contains the code that interacts with the server database.
The database structure can be better visualised in the Firebase console once some comments
have been added to the app. The basic structure of the database is that `sources` contain
`users` and `posts`, and `posts` contain `comments`.

File `backend/index.js` contains the code for the Cloud Functions.
They are in charge of periodically fetch the RSS feed.
They also update the counters for comments and likes.
Finally, these functions also send push notifications when a comment is liked or replied.

