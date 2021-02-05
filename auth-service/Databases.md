#Databases:
##clients:

- client-id
- client-secret (Optional)
- approved-redirect (list of urls)

## users:

- user-id
- verified
- email
- password
- password-salt
- Name
- picture

## users-validation:

- validation-code
- user-id

## users-external: (Look into this)

- external-id
- user-id
- provider : (google, facebook, soggard.us and so on)

## authentication-tokens

- auth-token (P key)
- client-id (R key)
- response-type :
- scope
- state
- nonce

## json-tokens

- token
- (other token keys)
- expires-at (epoc seconds)
