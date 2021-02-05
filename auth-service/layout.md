webserver?
GET login/
GET signup/
GET verify/

REST:
POST api/authenticate

- Input:
  - email
  - password
- Output:
  - status:
    - 302 - Login ok, Redirect
    - 401 - Bag login

POST api/create

- Input
- email
- password
- (MORE LATER)
- Out
- status:
- 200: ok, wait for email verification
- 409: email already registered
- 400: email or password is bad

POST api/verify

- Input:
  - Verification code
- Out
  - status:
    - 200: ok, redirect (if have)
    - 400: Unknown verification code

GET oauth2/authorize

- Check if we have an autorized user.
  - If we do redirect with 302 found back with access and renew token
  - If not redirect to the "GET login/"
- If response_type is not code (other option is toke) then reply 501 for not implemented
- Input
  - response_type = grant type
  - response_mode = query / fragment (How to create the response ? vs #), form_post and web_message should send 501 for not implemented
  - client_id
  - redirect_uri
  - scope
  - state

POST oauth2/token

- grant type:

  - client_credentials
    - Client login, get auth code from header auth
    - scope
      - What code is requested
  - password (We do not need to implement right away, just return 501)
    - Can only be used with client_id and client_secret provided and only select apps can use this
    - include "username and password"
  - authorization_code
    - code : the authentication token recived before
    - redirect_uri : (Optional, if provided it must match the excat redirect url provided at login)
    - client_id
  - refresh_token
    - scope - can be less, but not more
    -

- Response: 200 OK
- access_token :
- token_type : Just the string "bearer"
- expires_in : The amount of seconds from now
- refresh_token : (Only with code flow)
- scope : the scope granted
- Response headers:
- Cache-Control: no-store
- Pragma: no-cache
- 400 for error or bad token
  - error
    - https://www.oauth.com/oauth2-servers/access-tokens/access-token-response/
  - error_description

GET oauth2/callback

Get oauth2/.well-known/jwks.json
https://auth0.com/blog/navigating-rs256-and-jwks/

- List of keys
- alg: is the algorithm for the key
- kty: is the key type
- use: is how the key was meant to be used. For the example above, sig represents signature verification.
- x5c: is the x509 certificate chain
- e: is the exponent for a standard pem
- n: is the moduluos for a standard pem
- kid: is the unique identifier for the key
- x5t: is the thumbprint of the x.509 cert (SHA-1 thumbprint)

- RSAPrivateCrtKeySpec should be used in java
- https://developers.symphony.com/extension/docs/rsa-application-authentication-workflow
