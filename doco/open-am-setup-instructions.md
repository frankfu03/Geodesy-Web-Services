1) Login into admin console
  * username is `amadmin`
  * password is given by `credstash --profile geodesy -r ap-southeast-2 get DevGeodesyOpenAMAdminPassword`

## Add `isMemberOf` LDAP user attribute

1) Choose `Top Level Realm`

1) From navigation menu on the right, choose `Data Stores`

1) Choose `embedded` data store

1) In `LDAP User Attributes` add new value `isMemberOf`

1) At top of screen, click `Save`, then `Back to Data Stores`, then `Back to Access Control`

## Add some test users and groups

1) Choose `Top Level Realm`

1) From navigation manu on the right, choose `Subjects`

1) Create 3 new users

  * ID: user.a, user.b, user.x
  * Last Name: A, B, X
  * Full Name: User A, User B, User X
  * Password: gumby123A, gumby123B, gumby123X

1) Create 3 new groups

 * ID: edit-alic1, edit-alic2, superuser

1) Assign user.a to edit-alic1, user.b to edit-alic3, user.x to superuser

## Configure OAuth Provider

1) Choose `Top Level Realm`

2) From `Common Tasks`, choose `Configure OAuth Provider`

3) Choose `Configure OpenID Connect`

4) Click `Create`

5) From navigation menu on the right, choose `Services`

6) Choose `OAuth2 Provider`

7) Set `OAuth2 Token Signing Algorithm` to `RS256`

8) Set `Token Signing RSA public/private key pair to jwtSigningKey`

9) Turn on `Always return claims in ID Tokens`

9) Turn on `Allow clients to skip consent`

10) Click `Save Changes`

## Configure agent for GNSS Site Manager application

1) From navigation menu on the right, choose `Agents`

1) Select tab `OAuth 2.0/OpenID Connect Client`

1) In panel `Agent`, click `New...`

  * Name: GnssSiteManager
  * Password: gumby123

1) Click `Create`

1) Click on name of new agent, `GnssSiteManager`

1) Set `Client type` to `Public`

1) Add `https://dev.gnss-site-manager.geodesy.ga.gov.au/auth.html` and `http://localhost:5555/auth.html` to `Redirection URIs`

1) Add `openid` and `profile` to `Scopes`

1) Set `ID Token Signing Algorithm` to RS256

1) Add `https://dev.gnss-site-manager.geodesy.ga.gov.au` and `http://localhost:5555` to `Post Logout Redirect URIs`

1) Enable `Implied consent`

1) Click `Save`, `Back to Main Page`, and `Back to Access Control`

## Add extra fields to user (Organisation and Position)

1) Terminal into the OpenAM directory, eg 
    ```
    [if running inside docker] docker-compose exec -it open-am bash
    # cd /opt/openam
    ```
    
2)  edit the amUsers.xml file to add new atttributes
   `# vim config/xml/amUser.xml`
   
   ```xml  
    
   <AttributeSchema name="sunIdentityServerPPEmploymentIdentityOrg"
      type="single"
      syntax="string"
      any="display"
      i18nKey="Organisation">
    </AttributeSchema>

   <AttributeSchema name="sunIdentityServerPPEmploymentIdentityJobTitle"
      type="single"
      syntax="string"
      any="display"
      i18nKey="Position">
    </AttributeSchema>
   ```
3) delete and recreate the iPlanetAMUserService so that the above attributes get added

    ```
    # ./tools/admin/openam/bin/ssoadm \
        delete-svc \
        --adminid amadmin \
        --password-file tools/admin/passwdfile \
        --servicename iPlanetAMUserService 
        
    Service was deleted.
        
    # ./tools/admin/openam/bin/ssoadm \
        create-svc \
        --adminid amadmin \
        --password-file tools/admin/passwdfile \
        --xmlfile config/xml/amUser.xml
    
    Service was created.
   
    # exit        
    ```
4) rebuild the docker container
   ``` 
   $ docker-compose build open-am
   $ docker-compose up -d open-am
   ```
5) The Organisation and Position fields will now appear in the Subject screen in the OpenAM UI and will be stored in the LDAP attributes sunIdentityServerPPEmploymentIdentityOrg and sunIdentityServerPPEmploymentIdentityJobTitle respectively


## Configure OIDC claims script

1) Choose `Top Level Realm`

1) From navigation menu on the right choose, `Scripts`

1) Choose script `OIDC Claims Script`

1) Patch the groovy script with `oidc-claims-script.groovy.patch`

## Disable SDK caching

1) Configuration menu -> Server Defaults -> Advanced

2) Add property `com.iplanet.am.sdk.caching.enabled` with value `false`















