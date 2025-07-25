// config/jwk/generateToken.js
const fs  = require('fs');
const jwt = require('jsonwebtoken');

// Load your RSA private key
const key = fs.readFileSync('private.pem');

// Sign a JWT for sub="test-user", expires in 1h, with kid="my-key-id"
const token = jwt.sign(
  { sub: 'test-user' },
  key,
  {
    algorithm: 'RS256',
    expiresIn: '1h',
    keyid: 'my-key-id'
  }
);

console.log(token);
