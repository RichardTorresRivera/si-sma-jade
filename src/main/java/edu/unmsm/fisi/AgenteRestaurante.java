package edu.unmsm.fisi;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgenteRestaurante extends Agent {

    @Override
    protected void setup() {
        System.out.println("Hola, el Restaurante " + getAID().getLocalName() + " esta listo.");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("restaurante-service");
        sd.setName("Venta-de-comida");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println(getAID().getLocalName() + " registrado como 'restaurante-service'");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // COMPORTAMIENTO PARA RECIBIR PEDIDOS DEL CLIENTE
        addBehaviour(new EscucharPedidosBehaviour());
    }

    // Al finalizar el agente, se quita del registro del DF
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Restaurante " + getAID().getLocalName() + " cerrando.");
    }

    private class EscucharPedidosBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Filtramos para recibir solo mensajes de tipo REQUEST (Pedidos)
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String pedido = msg.getContent();
                jade.core.AID clienteAID = msg.getSender();
                System.out.println(getAID().getLocalName() + ": Recibi pedido de: " + pedido + " de " + msg.getSender().getLocalName());

                // Simular tiempo de preparación
                System.out.println(getAID().getLocalName() + ": Preparando comida...");

                addBehaviour(new WakerBehaviour(myAgent, 5000) {
                    @Override
                    protected void onWake() {
                        System.out.println(getAID().getLocalName() + ": Pedido '" + pedido + "' LISTO.");
                        // Una vez listo, buscar al coordinador para que asigne repartidor
                        notificarCoordinador(pedido, clienteAID);

                        ACLMessage respuestaCliente = msg.createReply();
                        respuestaCliente.setPerformative(ACLMessage.INFORM);
                        respuestaCliente.setContent("Tu pedido '" + pedido + "' esta listo y en camino.");
                        send(respuestaCliente);
                    }
                });
            } else {
                block();
            }
        }
    }

    private void notificarCoordinador(String pedido, jade.core.AID clienteAID) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("coordinador-service");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                ACLMessage aviso = new ACLMessage(ACLMessage.INFORM);
                aviso.addReceiver(result[0].getName());
                aviso.setContent("PEDIDO_LISTO#" + pedido + "#" + clienteAID.getLocalName());

                send(aviso);
                System.out.println(getAID().getLocalName() + ": Notificado al Coordinador. Cliente destino: " + clienteAID.getLocalName());
            } else {
                System.out.println(getAID().getLocalName() + ": Error: No se encontró Coordinador.");
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
