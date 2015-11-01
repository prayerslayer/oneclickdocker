var express = require('express'),
    _ = require('lodash'),
    superagent = require('superagent'),
    redis = require('redis'),
    Promise = require('bluebird'),
    winston = require('winston'),
    bodyParser = require('body-parser'),
    uuid = require('uuid'),
    app = express();


Promise.promisifyAll(redis.RedisClient.prototype);
Promise.promisifyAll(superagent);
var REDIS_CLIENT = redis.createClient();

REDIS_CLIENT.on('error', function() {
    winston.error.apply(winston, arguments);
});



app.use(bodyParser.json());
app.get('/', function(req, res) {
    return res.status(200).send("OK");
});

function defaultErrorHandler(res) {
    return function(err) {
        winston.error(err);
        return res.status(500).send(JSON.stringify(err));
    };
}

/**
 * GET /containers/:user: Fetches container with status, host, port, image
    GET /containers/:user/:container: Fetches idk? cpu load?
    POST /containers/:user: Put something in task queue
    DELETE /containers/:user/:container
 */

app.get('/containers/:user/?', function(req, res) {
    var user = req.params.user;
    Promise.map(
        REDIS_CLIENT.smembersAsync('USR;' + user),
        function(member) {
            return REDIS_CLIENT.hgetallAsync('CNT;' + user + ';' + member);
        })
    .reduce(
        function(acc, hash) {
            acc.push(hash);
            return acc;
        },
        [])
    .then(function(containers) {
        return res.status(200).send(containers);
    })
    .catch(defaultErrorHandler(res));
});

app.get('/containers/:user/:container/?', function(req, res) {
    var user = req.params.user,
        container = req.params.container;
    REDIS_CLIENT
    .hgetallAsync('CNT;' + user + ';' + container)
    .then(function(c) {
        if (!c) {
            throw new Error('NOT FOUND');
        }
        return res.status(200).send(c);
    })
    .catch(function(err) {
        return res.status(err.message === 'NOT FOUND' ? 404 : 500).send();
    });
});

app.post('/containers/:user/?', function(req, res) {
    // create task, put in queue
    var body = req.body,
        user = req.params.user;
    if (!body.image) {
        return res.status(400).send("Missing image property");
    }
    var id = uuid();
    body.user = user;

    REDIS_CLIENT
    .multi()
    .hmset(id, body)
    .rpush('TQUEUE', id)
    .exec(function(err) {
        if (err) {
            REDIS_CLIENT.del(id);
            return defaultErrorHandler(res)(err);
        }
        return res.status(201).send();
    });
});

app.delete('/containers/:user/:container/?', function(req, res) {
    // find out where the host is
    // send stop request there
    var user = req.params.user,
        container = req.params.container;
    REDIS_CLIENT
    .hgetallAsync('CNT;' + user + ';' + container)
    .then(function(c) {
        REDIS_CLIENT
        .hgetallAsync('HOST;' + c.host)
        .then(function(h) {
            superagent
            .post('http://' + h.ip + ':' + h.port + '/containers')
            .send({
                command: 'stop',
                user: user,
                container: container
            })
            .endAsync()
            .then(function() {
                return res.status(200).send();
            })
            .catch(defaultErrorHandler(res));
        })
        .catch(defaultErrorHandler(res));;
    })
    .catch(defaultErrorHandler(res));;
});

app.listen(process.env.PORT || 3000);