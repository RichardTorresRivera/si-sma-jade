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
import java.util.List;

public class CoordinatorAgent extends Agent {
    private List<ACLMessage> orders = new ArrayList<>();
    private int deliverySendMessages = 0;
    private List<ACLMessage> deliveryReplies = new ArrayList<>();

    protected void setup() {
        ServiceDescription coordinatorService = new ServiceDescription();
        coordinatorService.setType("coordinador-service");
        coordinatorService.setName(this.getLocalName());

        ServiceDescription deliveryService = new ServiceDescription();
        deliveryService.setType("delivery");
        deliveryService.addProperties(new Property("estado", "disponible"));

        registerService(coordinatorService);
        handleRestMessage(
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

    protected void handleRestMessage(
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
                        // Reply rest
                        ACLMessage reply = msg.createReply();
                        reply.setContent(restReply);
                        myAgent.send(reply);

                        orders.add(msg);

                        searchDelivery(delService);

                    } else if (msg.getPerformative() == ACLMessage.PROPOSE || msg.getPerformative() == ACLMessage.REFUSE) {

                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            System.out.println("Propuesta de " + msg.getSender().getLocalName() + " => " + msg.getContent());
                        } else {
                            System.out.println("Delivery " + msg.getSender().getLocalName() + " => no disponible");
                        }

                        deliveryReplies.add(msg);

                        if (deliveryReplies.size() == deliverySendMessages) {
                            int bestIndex = getBestIndexDelivery();

                            if (bestIndex != -1) {
                                for (int i = 0; i < deliveryReplies.size(); i++) {
                                    ACLMessage currentReplyMsg = deliveryReplies.get(i);

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
                                        myAgent.send(answer);
                                    }
                                }
                                if (!orders.isEmpty()) {
                                    orders.remove(0);
                                }
                            } else {
                                System.out.println("Todos los delivery están ocupados");
                            }

                            deliveryReplies.clear();
                            deliverySendMessages = 0;
                        }
                    } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().equals("estoy disponible")) {
                        if (!orders.isEmpty()) {
                            System.out.println(msg.getSender().getLocalName() + " está libre. Retomando pedido pendiente en la cola...");
                            searchDelivery(delService);
                        }
                    }
                } else {
                    block();
                }
            }
        });
    }

    protected int getBestIndexDelivery() {
        int bestIndex = -1;
        int bestDeliveryTime = Integer.MAX_VALUE;

        for (int i = 0; i < deliveryReplies.size(); i++) {
            ACLMessage reply = deliveryReplies.get(i);

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

    protected void searchDelivery(ServiceDescription sd) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.addServices(sd);

        try {
            DFAgentDescription[] res = DFService.search(this, dfd);

            if (res.length > 0) {
                for (DFAgentDescription delivery : res) {
                    ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                    msg.addReceiver(delivery.getName());
                    msg.setContent("solicitado delivery");
                    this.send(msg);

                    deliverySendMessages++;
                }
            } else {
                System.out.println("No hay deliveries disponibles");
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
