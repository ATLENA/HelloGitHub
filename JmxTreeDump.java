import com.sun.tools.attach.VirtualMachine;
import javax.management.*;
import javax.management.remote.*;
import java.util.*;

public class JmxTreeDump {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: JmxTreeDump <pid>");
            return;
        }

        String pid = args[0];
        VirtualMachine vm = VirtualMachine.attach(pid);

        // 로컬 JMX agent 없으면 시작
        Properties props = vm.getSystemProperties();
        String connectorAddr = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        if (connectorAddr == null) {
            String agent = props.getProperty("java.home") + "/lib/management-agent.jar";
            vm.loadAgent(agent);
            connectorAddr = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        }

        JMXServiceURL url = new JMXServiceURL(connectorAddr);
        JMXConnector connector = JMXConnectorFactory.connect(url);
        MBeanServerConnection mbsc = connector.getMBeanServerConnection();

        Set<ObjectName> names = mbsc.queryNames(null, null);

        // domain → type → name 트리 빌드
        Map<String, Map<String, List<String>>> tree = new TreeMap<>();
        for (ObjectName on : names) {
            String domain = on.getDomain();
            if (domain == null || domain.isBlank()) {
                domain = "(default)";
            }
            String type = on.getKeyProperty("type");
            if (type == null || type.isBlank()) {
                type = "(unknown)";
            }
            String name = on.getKeyProperty("name");
            if (name == null || name.isBlank()) {
                name = "(unnamed)";
            }

            tree.putIfAbsent(domain, new TreeMap<>());
            tree.get(domain).putIfAbsent(type, new ArrayList<>());
            tree.get(domain).get(type).add(name);
        }

        // 출력
        for (var domain : tree.keySet()) {
            System.out.println(domain);
            for (var type : tree.get(domain).keySet()) {
                System.out.println("  └─ type=" + type);
                for (var name : tree.get(domain).get(type)) {
                    System.out.println("       └─ name=" + name);
                }
            }
        }

        connector.close();
        vm.detach();
    }
}
