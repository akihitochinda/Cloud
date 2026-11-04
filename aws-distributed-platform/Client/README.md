### Run instructions for Performance Analysis (From your local machine)

#### Local Tomcat

1. Download `Assignment1` from Git Repository URL
2. Set environment variable `CATALINA_HOME` & `JAVA_HOME`
   1. `set CATALINA_HOME=C:\Program Files\Apache Software Foundation\Tomcat 9.0`
   2. `set JAVA_HOME=C:\Users\%USERNAME%\.jdks\corretto-11.0.32.1`

#### Verification

```bash
# Start Tomcat
/Library/Tomcat/bin/startup.sh  # macOS
# OR
C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\startup.bat  # Windows

# Test in browser
open http://localhost:8080
```
**Expected Result:** Tomcat welcome page appears.

#### Configure Tomcat in IntelliJ (From Assignment1\server Project)

1. Clean Previous Deployments

```bash
# Remove any existing deployments
sudo rm -rf /opt/tomcat9/webapps/skitrak-server*
sudo rm -rf /opt/tomcat9/work/Catalina/localhost/skitrak-server*
```

2. Add Tomcat Server Configuration
   1. **Run** → **Edit Configurations**
   2. Click **+** → **Tomcat Server** → **Local**
   3. **Configuration:**
      - **Name:** `Tomcat 9 - Assignment 1`
      - **Tomcat Home:** `/Library/Tomcat` (or your Windows path)
      - **JRE:** Java 11
      - **HTTP Port:** 8080
   4. In same dialog, click **Deployment** tab
   5. Click **+** → **Artifact** → **skitrak-server:war exploded**
   6. **Application Context:** `/skitrak-server`
   7. Click **OK**

#### Test Deployed Application

1. Click **Run** button (green triangle)
2. IntelliJ should:
   - Start Tomcat
   - Deploy your application
   - Open browser to http://localhost:8080/skitrak-server

Expected Result: Your index.jsp page displays with current time.

#### Performance Analysis (From Assignment1\client-part2 Project)

1. Add Application Configuration
   1. **Run** → **Edit Configurations**
   2. Click **+** → **Application**
      - **Name:** `Main`
      - **Main class** → **Browse**→ **Main** (skitrak-client)
      - **Program arguments:** `http://localhost:8080/skitrak-server`
   3. Click **OK**

2. Click **Run** button (green triangle)
3. IntelliJ should:
   - Start Main console
   - Start Performance Analysis (printed on completion)
   - Start Statistical Analysis (printed and in report)

Expected Result: Your swipe_metrics.csv / throughput.csv reports store in Assignment1\results folder