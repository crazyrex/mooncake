var express = require('express');
var app = express();

app.set('port', (process.env.PORT || 7070));

var pageData = {
  "javascriptsBase": "/javascripts",
  "stylesheetsBase": "/stylesheets",
  "imagesBase": "/images"
};

app.use('/', express.static(__dirname + '/resources/public'));

app.set('view engine', 'jade');
app.set('views', './assets/jade');

function beforeAllFilter(req, res, next) {
  app.locals.pretty = true;

  next();
}

app.all('*', beforeAllFilter);

function customRender(res, template, data) {
  res.render(template, data, function (err, html) {
    var cleanHTML = html.replace(/>!/g, '>');
    res.send(cleanHTML);
  });
}

app.all('/', function(req, res){
  customRender(res, 'sign-in', pageData);
});

app.get(['/index', '/feed'], function(req, res){
  customRender(res, 'feed', pageData);
});

app.get('/empty-feed', function(req, res){
  customRender(res, 'empty-feed', pageData);
});

app.get('/routes', function(req, res){
  customRender(res, 'routes', pageData);
});

app.get('/sign-in', function(req, res){
  customRender(res, 'sign-in', pageData);
});

app.get('/create-account', function(req, res) {
  customRender(res, 'create-account', pageData);
});

app.get('/customise-feed', function(req, res) {
  customRender(res, 'customise-feed', pageData);
});

app.get('/error-500', function(req, res) {
  customRender(res, 'error-500', pageData);
});

app.get('/library', function(req, res) {
  customRender(res, 'library', pageData);
});

app.get('/sign-out', function(req, res) {
  res.redirect('./');
});

app.listen(app.get('port'), function() {
  console.log('Node app is running on port', app.get('port'));
});
