**How to configure auto login for crawler (apply for private content page)?**

1. Create a user, have its membership in the target site. Inspect `userId` from user details screen. Make sure you're able to login with that user to portal, and complete all steps at first time login.

2. From **Control Panel > Server Administration > Script**, run script [crawler-id-generate.groovy](crawler-id-generate.groovy). The result will give a hashed ID and a hashed password.
- Before running the Groovy script above, remember to enter the correspond `userId`, `plainPassword` and `companyId`.
- To find `companyId`, go to **Control Panel > Virtual Instances**, grab the value of column `Instance ID`.

3. Go to **Control Panel > System Settings > connect > connect-crawler-configuration**
- Make sure `auto-authenticate` is checked.
- Enter hashed ID and hashed password to corresponding fields.
- Save the configuration.

**Known issues:**

- TODO: improve the indexed content/summary to show the most relevant words.
