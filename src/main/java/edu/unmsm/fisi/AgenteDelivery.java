package edu.unmsm.fisi;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import java.util.Random;

public class AgenteDelivery extends Agent{    
     private boolean ocupado = false;
     private int tiempoEstimado = 0;

     protected void setup() {
          // Leer argumentos desde la terminal
          Object[] args = getArguments();
          if(args != null && args.length > 0) {
               ocupado = Boolean.parseBoolean(args[0].toString());
          }
          
          // Descripción del servicio
          ServiceDescription servicio = new ServiceDescription();
          // Su servicio es responder con disponibilidad
          servicio.setType("delivery");
          servicio.setName(this.getLocalName());

          // Agregando propiedad de disponibilidad
          servicio.addProperties(new Property("estado", ocupado ? "ocupado": "disponible"));
          
          registrarServicio(servicio);
          recibirMensajes();
     }

     // Método para registrar servicio
     protected void registrarServicio(ServiceDescription sd) {
          DFAgentDescription dfd = new DFAgentDescription();
          dfd.addServices(sd);
          try {
               DFService.register(this, dfd);
          } catch (FIPAException e) {
               e.printStackTrace();
          }
     }

     // Método para añadir un comportamiento que recibe mensajes
     protected void recibirMensajes() {
          addBehaviour(new CyclicBehaviour(this) {
               public void action() {
                    ACLMessage msg = receive();
                    if (msg != null) {

                         if (msg.getPerformative() == ACLMessage.CFP) {
                              ACLMessage reply = msg.createReply();

                              if (ocupado) {
                                   reply.setPerformative(ACLMessage.REFUSE);
                                   reply.setContent("Estoy ocupado con otra entrega");
                              } else {
                                   reply.setPerformative(ACLMessage.PROPOSE);
                                   Random rand = new Random();
                                   tiempoEstimado = rand.nextInt(16) + 5;
                                   reply.setContent("Tiempo estimado: " + tiempoEstimado + " minutos");
                                   actualizarDisponibilidadDF(true);
                              }
                              myAgent.send(reply);
                         } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                              simularViaje(msg);
                         } else if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                              actualizarDisponibilidadDF(false);

                              ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                              reply.addReceiver(msg.getSender());
                              reply.setContent("estoy disponible");
                              myAgent.send(reply);
                         }
                    } else {
                         block();
                    }
               }
          });
     }

     private void simularViaje(ACLMessage senderMsg) {
          // Convertir minutos a segundos
          long tiempoEsperaMS = tiempoEstimado * 1000L;
          System.out.println(this.getLocalName() + " inicio viaje.");

          // Cambiar estado interno y en el DF

          addBehaviour(new WakerBehaviour(this, tiempoEsperaMS) {
               @Override
               protected void onWake() {
                    actualizarDisponibilidadDF(false);
                    System.out.println(myAgent.getLocalName() + ": Pedido listo. Entrega finalizada");

                    ACLMessage entrega = new ACLMessage(ACLMessage.INFORM);
                    entrega.addReceiver(senderMsg.getSender());
                    entrega.setContent("estoy disponible");
                    myAgent.send(entrega);
               }
          });
     }

     private void actualizarDisponibilidadDF(boolean estaOcupado) {
          this.ocupado = estaOcupado;

          DFAgentDescription dfd = new DFAgentDescription();
          dfd.setName(getAID());

          ServiceDescription sd = new ServiceDescription();
          sd.setType("delivery");
          sd.setName(getLocalName());
          sd.addProperties(new Property("estado", ocupado ? "ocupado": "disponible"));

          dfd.addServices(sd);

          try {
               DFService.modify(this, dfd);
          } catch (FIPAException e) {
               e.printStackTrace();
          }
     }
}
