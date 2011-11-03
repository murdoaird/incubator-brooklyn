package brooklyn.entity.webapp.jboss

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.with
import groovy.lang.MetaClass

import java.util.concurrent.TimeUnit

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import brooklyn.entity.Entity
import brooklyn.entity.webapp.JavaWebAppService
import brooklyn.entity.webapp.JavaWebAppSoftwareProcess
import brooklyn.event.adapter.HttpSensorAdapter
import brooklyn.event.basic.BasicAttributeSensor
import brooklyn.event.basic.ConfiguredAttributeSensor
import brooklyn.location.basic.SshMachineLocation
import brooklyn.util.flags.SetFromFlag

public class JBoss7Server extends JavaWebAppSoftwareProcess implements JavaWebAppService {

	public static final Logger log = LoggerFactory.getLogger(JBoss7Server.class)
	
    @SetFromFlag("managementPort")
    public static final ConfiguredAttributeSensor<Integer> MANAGEMENT_PORT = 
            [ Integer, "http.managementPort", "Management port", 9990 ]

    public static final BasicAttributeSensor<Integer> MANAGEMENT_STATUS =
            [ Integer, "webapp.http.managementStatus", "HTTP response code for the management server" ]
    
			
    public JBoss7Server(Map flags=[:], Entity owner=null) {
        super(flags, owner)
    }
		
	@Override	
	protected void connectSensors() {
		super.connectSensors();

		def host = getAttribute(HOSTNAME)
		def port = getAttribute(MANAGEMENT_PORT)
		def http = sensorRegistry.register(
			new HttpSensorAdapter("http://$host:$port/management/subsystem/web/connector/http/read-resource", period: 200*TimeUnit.MILLISECONDS).
				vars("include-runtime":null) )
		with(http) {
			poll(MANAGEMENT_STATUS, { responseCode })
			poll(SERVICE_UP, { responseCode==200 })

			poll(REQUEST_COUNT) { json.requestCount }
			poll(ERROR_COUNT) { json.errorCount }
			poll(TOTAL_PROCESSING_TIME) { json.processingTime }
			poll(MAX_PROCESSING_TIME) { json.maxTime }
			poll(BYTES_RECEIVED) { json.bytesReceived }
			poll(BYTES_SENT, { json.bytesSent })
		}
	}

    public JBoss7SshDriver newDriver(SshMachineLocation machine) { return new JBoss7SshDriver(this, machine) }

}
