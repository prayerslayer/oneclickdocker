var express = require('express'),
    superagent = require('superagent'),
    cookieParser = require('cookie-parser'),
    cookieSession = require('cookie-session'),
    Promise = require('bluebird'),
    bodyParser = require('body-parser'),
    session = require('express-session'),
    authRoutes = require('./routes/auth'),
    indexRoute = require('./routes/index'),
    passport = require('passport'),
    GithubLogin = require('passport-github').Strategy,
    app = express();


function loggedIn(req, res, next) {
    if (!!req.user) {
        next();
    }
    return res.redirect('/');
}

app.use(express.static(__dirname + '/public'));
app.use(bodyParser.urlencoded({
    extended: true
}));
app.use(cookieParser());
app.use(cookieSession({
    keys: ['key1', 'key2']
}));
app.use(passport.initialize());
app.use(passport.session());
app.set('view engine', 'jade');

passport.use(new GithubLogin({
    clientID: process.env.GH_CLIENT_ID,
    clientSecret: process.env.GH_CLIENT_SECRET,
    callbackURL: 'http://localhost:4000/auth/github/callback'
}, function(accessToken, refreshToken, profile, done){
    done(null, profile);
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
app.get('/error', function(req, res) {
    res.render('error');
});

function dashboard(req, res) {
    res.render('dashboard', {
        user: req.user
    });
};

app.get('/dashboard', dashboard);

app.get('/start-container', dashboard);
app.post('/start-container', loggedIn, function(req, res) {
    superagent
    .post('http://localhost:4322/containers/' + req.user.username)
    .send({
        user: req.user.username,
        image: req.body.image
    })
    .end();
});

app.listen(4000);