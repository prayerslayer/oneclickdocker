var express = require('express'),
    cookieParser = require('cookie-parser'),
    session = require('express-session'),
    authRoutes = require('./routes/auth'),
    indexRoute = require('./routes/index'),
    passport = require('passport'),
    GithubLogin = require('passport-github').Strategy,
    app = express();


app.use(cookieParser());
app.use(session({secret: process.env.SESSION_SECRET || 'mysecret'}));
app.use(passport.initialize());
app.use(passport.session());
app.set('view engine', 'jade');

passport.use(new GithubLogin({
    clientID: process.env.GH_CLIENT_ID,
    clientSecret: process.env.GH_CLIENT_SECRET,
    callbackURL: 'http://localhost:4000/auth/github/callback'
}, function(accessToken, refreshToken, profile, done){
    done(null, {
        profile: profile
    });
}));

passport.serializeUser(function(user, done) {
    done(null, user);
});

passport.deserializeUser(function(user, done) {
    done(null, user);
});

app.get('/auth/error', authRoutes.error);
app.get('/auth/github', passport.authenticate('github'));
app.get('/auth/github/callback',
    passport.authenticate('github', {failureRedirect: '/auth/error'}),
    authRoutes.success);

app.get('/', indexRoute);

app.listen(4000);