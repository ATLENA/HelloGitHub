import com.sun.tools.attach.VirtualMachine;
import javax.management.*;
import javax.management.remote.*;
 
public class JmxTypeDump {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: JmxTypeDump <pid> <type>");
            return;
        }
 
        String pid = args[0];
        String type = args[1];
 
        VirtualMachine vm = VirtualMachine.attach(pid);
        String addr = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        if (addr == null) {
            String agent = vm.getSystemProperties().getProperty("java.home") + "/lib/management-agent.jar";
            vm.loadAgent(agent);
            addr = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        }
        JMXConnector conn = JMXConnectorFactory.connect(new JMXServiceURL(addr));
        MBeanServerConnection mbsc = conn.getMBeanServerConnection();
 
        for (ObjectName on : mbsc.queryNames(new ObjectName("*:type=" + type + ",*"), null)) {
            System.out.println("MBean: " + on);
            for (MBeanAttributeInfo attr : mbsc.getMBeanInfo(on).getAttributes()) {
                try {
                    Object val = mbsc.getAttribute(on, attr.getName());
                    System.out.println("  " + attr.getName() + " = " + val);
                } catch (Exception e) {
                    System.out.println("  " + attr.getName() + " = (error: " + e.getMessage() + ")");
                }
            }
            System.out.println();
        }
 
        conn.close();
        vm.detach();
    }
}
