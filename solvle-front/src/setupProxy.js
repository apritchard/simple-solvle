const { createProxyMiddleware } = require("http-proxy-middleware");
module.exports = function(app) {
    const isDocker = process.env.DOCKER_ENV;
    const target = isDocker ?
        'http://server:8081' :
        'http://localhost:8081';
    app.use(createProxyMiddleware('/solvle',
        {
            target: target,
            changeOrigin: true
        }
        ));
    app.use(createProxyMiddleware('/solvescape',
        {
            target: target,
            changeOrigin: true
        }
        ));
}