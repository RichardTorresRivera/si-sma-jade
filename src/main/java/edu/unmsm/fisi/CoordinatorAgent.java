package edu.unmsm.fisi;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CoordinatorAgent extends Agent {
    private List<ACLMessage> orders = new ArrayList<>();
    private HashMap<String, Integer> expectedReplies = new HashMap<>();
    private HashMap<String, List<ACLMessage>> delReplies = new HashMap<>();

    protected void setup() {
        ServiceDescription coordinatorService = new ServiceDescription();
        coordinatorService.setType("coordinador-service");
        coordinatorService.setName(this.getLocalName());

        ServiceDescription deliveryService = new ServiceDescription();
        deliveryService.setType("delivery");
        // deliveryService.addProperties(new Property("estado", "disponible"));

        registerService(coordinatorService);
        handleRecMessage(
                "pedido_listo:",
                "buscando delivery",
                "delivery asignado",
                deliveryService
        );
    }

    protected void registerService(ServiceDescription sd) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    protected void handleRecMessage(
            String restMessage,
            String restReply,
            String delReply,
            ServiceDescription delService
    ) {
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();

                if (msg != null) {
                    if (msg.getContent().toLowerCase().contains(restMessage)) {
                        ACLMessage reply = msg.createReply();
                        String convId = "conv-" + java.util.UUID.randomUUID().toString().substring(0,8);

                        msg.setConversationId(convId);
                        reply.setContent(restReply);
                        myAgent.send(reply);

                        orders.add(msg);

                        searchDelivery(delService, convId);

                    } else if (msg.getPerformative() == ACLMessage.PROPOSE || msg.getPerformative() == ACLMessage.REFUSE) {
                        String convId = msg.getConversationId();

                        if (convId != null && expectedReplies.containsKey(convId)) {
                            if (msg.getPerformative() == ACLMessage.PROPOSE) {
                                System.out.println("Propuesta de " + msg.getSender().getLocalName() + " para " + convId + " => " + msg.getContent());
                            } else {
                                System.out.println("Delivery " + msg.getSender().getLocalName() + " => no disponible para " + convId);
                            }

                            delReplies.get(convId).add(msg);

                            if (expectedReplies.get(convId) == delReplies.get(convId).size()) {
                                assignDelivery(delReply, convId);
                            }
                        }

                    } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().equals("estoy disponible")) {
                        if (!orders.isEmpty()) {
                            for (ACLMessage order : orders) {
                                String id = order.getConversationId();

                                if(!expectedReplies.containsKey(id)) {
                                    System.out.println(msg.getSender().getLocalName() + " está libre. Retomando pedido pendiente en la cola...");
                                    searchDelivery(delService, id);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    block();
                }
            }
        });
    }

    protected int getBestIndexDelivery(List<ACLMessage> replies) {
        int bestIndex = -1;
        int bestDeliveryTime = Integer.MAX_VALUE;

        for (int i = 0; i < replies.size(); i++) {
            ACLMessage reply = replies.get(i);

            if (reply.getPerformative() == ACLMessage.PROPOSE) {
                try {
                    int currentDeliveryTime = Integer.parseInt(reply.getContent().split(" ")[2]);

                    if (currentDeliveryTime < bestDeliveryTime) {
                        bestIndex = i;
                        bestDeliveryTime = currentDeliveryTime;
                    }
                } catch (Exception e) {
                    System.out.println("Error leyendo el tiempo de: " + reply.getSender().getLocalName());
                }
            }
        }

        return bestIndex;
    }

    protected void searchDelivery(ServiceDescription sd, String convId) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd);

        try {
            DFAgentDescription[] res = DFService.search(this, dfd);

            if (res.length > 0) {
                delReplies.put(convId, new ArrayList<>());
                expectedReplies.put(convId, res.length);

                for (DFAgentDescription delivery : res) {
                    ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                    msg.addReceiver(delivery.getName());
                    msg.setContent("solicitado delivery");
                    msg.setConversationId(convId);
                    this.send(msg);
                }
            } else {
                System.out.println("No hay deliveries disponibles para " + convId);
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    protected void assignDelivery(String delReply, String convId) {
        List<ACLMessage> replies = delReplies.get(convId);
        int bestIndex = getBestIndexDelivery(replies);

        if (bestIndex != -1) {
            for (int i = 0; i < replies.size(); i++) {
                ACLMessage currentReplyMsg = replies.get(i);

                if (currentReplyMsg.getPerformative() == ACLMessage.PROPOSE) {
                    ACLMessage answer = currentReplyMsg.createReply();

                    if (i == bestIndex) {
                        answer.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        answer.setContent(delReply);
                        System.out.println("Asignando pedido a " + currentReplyMsg.getSender().getLocalName());
                    } else {
                        answer.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        answer.setContent("Pedido asignado a otro");
                    }
                    this.send(answer);
                }
            }

            orders.removeIf(order -> order.getConversationId() != null && order.getConversationId().equals(convId));
        } else {
            System.out.println("Todos los delivery rechazaron el " + convId);
        }

        delReplies.remove(convId);
        expectedReplies.remove(convId);
    }
}
