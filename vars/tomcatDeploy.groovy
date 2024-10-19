def call() {
    sshagent(['tomcat-deploy']) {
        sh '''
            # Define variables for clarity and reusability
            TOMCAT_USER=ec2-user
            TOMCAT_HOST=172.31.43.162
            
            TOMCAT_PATH=/opt/tomcat9
            WAR_FILE=target/ai-leads.war

            # Check if the WAR file exists before proceeding
            if [ ! -f "$WAR_FILE" ]; then
                echo "WAR file not found: $WAR_FILE"
                exit 1
            fi

            # Check if the EC2 instance is reachable
            ping -c 2 $TOMCAT_HOST > /dev/null 2>&1
            if [ $? -ne 0 ]; then
                echo "EC2 instance $TOMCAT_HOST is not reachable"
                exit 1
            fi

            # Copy the WAR file to the Tomcat webapps directory
            scp -o StrictHostKeyChecking=no $WAR_FILE $TOMCAT_USER@$TOMCAT_HOST:$TOMCAT_PATH/webapps
            if [ $? -ne 0 ]; then
                echo "Failed to copy WAR file to Tomcat server"
                exit 1
            fi

            # Shutdown Tomcat (ignore errors if already stopped)
            ssh -o StrictHostKeyChecking=no $TOMCAT_USER@$TOMCAT_HOST "if [ -f $TOMCAT_PATH/bin/shutdown.sh ]; then $TOMCAT_PATH/bin/shutdown.sh || true; else echo 'Shutdown script not found'; fi"

            # Start Tomcat
            ssh -o StrictHostKeyChecking=no $TOMCAT_USER@$TOMCAT_HOST "if [ -f $TOMCAT_PATH/bin/startup.sh ]; then $TOMCAT_PATH/bin/startup.sh; else echo 'Tomcat startup script not found'; exit 1; fi"
        '''
    }
}
