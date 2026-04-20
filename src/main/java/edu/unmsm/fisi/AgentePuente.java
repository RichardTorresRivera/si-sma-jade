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

        if (args != null && args.length > 0) {
            String remoteName = (String) args[0];
            remoteAgentAID = new AID(remoteName, AID.ISGUID);
            remoteAgentAID.addAddresses("http://plataforma-b:7778/acc");

            registrarServicioLocal();

            addBehaviour(new CyclicBehaviour() {
                public void action() {
                    ACLMessage msg = receive();

                    if (msg != null) {
                        System.out.println(
                            "[Puente] Recibido de: " + msg.getSender().getName() +
                            " | Contenido: " + msg.getContent() +
                            " | ConvID: " + msg.getConversationId()
                        );
                        // MENSAJE DESDE PLATAFORMA A → reenviar a B
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
                                " | ConvID: " + convId
                            );
                        }
                        // RESPUESTA DESDE B → devolver a A
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
        }
    }

    private void registrarServicioLocal() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("apaga fuego");
        sd.setName("Servicio-Remoto-Bombero");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}