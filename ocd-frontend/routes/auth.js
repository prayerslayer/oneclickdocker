var db = require('../db');

module.exports.success = function(req, res) {
    db
    .saveUserAsync([
        req.user.profile.id,
        req.user.profile.username,
        req.user.profile.emails[0].value,
        req.user.profile.displayName,
        null,
        null
    ])
    .then(function() {
        res.redirect('/');
    })
    .catch(function(err) {
        return res.status(500).send(JSON.stringify(err));
    });
};

module.exports.error = function(req, res) {
    return res.status(500).send('Login failed');
};