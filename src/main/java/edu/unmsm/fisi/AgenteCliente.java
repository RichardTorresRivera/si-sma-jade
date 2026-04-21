package edu.unmsm.fisi;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class AgenteCliente extends Agent {
    protected void setup() {
        // Capturar pedido
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String pedido = (String) args[0];

            System.out.printf("[Cliente: %s] - Informacion del cliente: %n", getLocalName());
            System.out.printf("- Nombre global: %s%n", getAID().getName());
            System.out.println("------------------------------------------");
            ServiceDescription cliente_servicio = new ServiceDescription();
            cliente_servicio.setType("restaurante-service");
            buscarServicioRestaurante(cliente_servicio, pedido);
            addBehaviour(new CyclicBehaviour() {
                @Override
                public void action() {
                    ACLMessage msg = receive();
                    if (msg != null) {
                        System.out.println("[Cliente: %s] - Mensaje recibido");
                        System.out.printf("- Emisor: %s", msg.getSender().getName());
                        System.out.printf("- Mensaje: %s", msg.getContent());
                        System.out.println("------------------------------------------");
                    }
                    else block();
                }
            });
        }

    }

    protected void buscarServicioRestaurante(ServiceDescription sd, String pedido){
        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            protected void onTick() {
                DFAgentDescription dfd = new DFAgentDescription();
                dfd.addServices(sd);

                try {
                    DFAgentDescription [] res = DFService.search(myAgent, dfd);
                    if (res.length != 0){
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.addReceiver(res[0].getName());
                        msg.setContent(pedido);
                        myAgent.send(msg);
                        stop();
                    } else {
                        System.out.println("No hay restaurantes disponibles");
                    }
                } catch (FIPAException e){
                    e.printStackTrace();
                }
            }
        });
    }

    protected void takeDown(){
        System.out.printf("[Cliente: %s] - Cliente se despide%n", getLocalName());
    }
}
