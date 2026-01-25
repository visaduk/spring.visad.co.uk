# How to Allow Remote Database Access in cPanel

To allow this application server (`172.31.26.12`) to connect to your cPanel database, you need to whitelist its IP address.

## Step 1: Log in to cPanel
Log in to your cPanel account on the server `78.128.8.203`.

## Step 2: Find "Remote MySQL"
1.  Scroll down to the **Databases** section.
2.  Click on the icon labeled **Remote MySQL** (or "Remote MySQL®").

## Step 3: Add the Access Host
You will see a section named **Add Access Host**.
1.  **Host (% wildcard is allowed):** Enter the IP address of this application server:
    ```
    172.31.26.12
    ```
    *(If that doesn't work, you can try `172.31.26.%` to whitelist the subnet, or just `%` to allow ALL IPs for testing temporarily, but remove `%` later for security).*
2.  **Comment (optional):** You can write "App Server" here.
3.  Click the **Add Host** button.

## Step 4: Verify Firewall (If applicable)
If you still cannot connect after adding the IP to Remote MySQL:
-   If you have **ConfigServer Security & Firewall (CSF)** installed in WHM/cPanel (usually under "Plugins" or "Security"), check if port `3306` is open or if the IP `172.31.26.12` is blocked.
-   However, Step 3 is usually sufficient for standard shared hosting cPanel accounts.

## Step 5: Retest
Or tell me to retry the connection after you have added the IP.
