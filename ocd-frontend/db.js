var pg = require('pg'),
    sqlt = require('sqlt'),
    Promise = require('bluebird'),
    queries = sqlt.dir('db'),
    PG_CONN = new pg.Client(process.env.PG_CONN_STRING);

PG_CONN.connect();

module.exports = Promise.promisifyAll(
                    Object
                    .keys(queries)
                        .reduce(function(prev, query) {
                            prev[query] = queries[query].bind(queries, PG_CONN);
                            return prev;
                        }, {}));

module.exports.client = PG_CONN;
