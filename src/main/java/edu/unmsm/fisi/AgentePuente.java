package edu.unmsm.fisi;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;

public class AgentePuente extends Agent {

    private AID remoteAgentAID;
    private Map<String, AID> conversaciones = new HashMap<>();

    protected void setup() {
        Object[] args = getArguments();

        if (args != null && args.length >= 4) {
            String agentName = (String) args[0];
            String platformName = (String) args[1];
            String serviceType = (String) args[2];
            String serviceName = (String) args[3];
            remoteAgentAID = new AID(agentName + "@" + platformName, AID.ISGUID);
            remoteAgentAID.addAddresses("http://" + platformName + ":7778/acc");

            registrarServicioLocal(serviceType, serviceName);

            addBehaviour(new CyclicBehaviour() {
                public void action() {
                    ACLMessage msg = receive();

                    if (msg != null) {
                        System.out.println(
                            "[Puente] Mensaje recibido de: " + msg.getSender().getName() +
                            "\n\t| Contenido: " + msg.getContent() +
                            "\n\t| ConvID: " + msg.getConversationId()
                        );
                        // Mensaje desde plataforma ORIGEN --> Reenviar a DESTINO
                        if (!msg.getSender().equals(remoteAgentAID)) {

                            String convId = "conv-" + System.currentTimeMillis();

                            conversaciones.put(convId, msg.getSender());

                            ACLMessage forward = new ACLMessage(msg.getPerformative());
                            forward.addReceiver(remoteAgentAID);
                            forward.setContent(msg.getContent());
                            forward.setConversationId(convId);

                            send(forward);

                            System.out.println(
                                "[Puente] → Enviado a remoto: " + remoteAgentAID.getName() +
                                "\n\t| ConvID: " + convId
                            );
                        }
                        // Respuesta desde plataforma DESTINO --> Reenviar a ORIGEN
                        else {
                            String convId = msg.getConversationId();
                            AID original = conversaciones.get(convId);

                            System.out.println(
                                "[Puente] ← Respuesta de remoto | ConvID: " + convId
                            );

                            if (original != null) {
                                ACLMessage reply = new ACLMessage(msg.getPerformative());
                                reply.addReceiver(original);
                                reply.setContent(msg.getContent());

                                send(reply);

                                System.out.println(
                                        "[Puente] → Reenviado a: " + original.getName()
                                );

                                conversaciones.remove(convId);
                            } else {
                                System.out.println(
                                    "[Puente] No se encontró conversación para ConvID: " + convId
                                );
                            }
                        }

                        if (msg.getSender().getLocalName().equals("ams")) {
                            System.out.println("[Puente] ERROR recibido del AMS, no reintentar");
                            return;
                        }
                    } else {
                        block();
                    }
                }
            });
        } else {
            System.out.println("[Error] Falta argumentos para el Agente Puente");
            System.out.println("\t1: Nombre del agente");
            System.out.println("\t2: Nombre de la plataforma");
            System.out.println("\t3: Tipo de servicio");
            System.out.println("\t4: Nombre del servicio");
        }
    }

    private void registrarServicioLocal(String serviceType, String serviceName) {
        // Crear la descripcion del servicio
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType);
        sd.setName(serviceName);

        // Crear la descripcion del agente
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd);

        // Registrar en el DF
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}