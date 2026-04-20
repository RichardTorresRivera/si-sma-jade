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
                         if (ocupado) {
                              ACLMessage reply = msg.createReply();
                              reply.setPerformative(ACLMessage.REFUSE);
                              reply.setContent("Estoy ocupado con otra entrega");
                              myAgent.send(reply);
                         } else {
                              Random rand = new Random();
                              int tiempoEstimado = rand.nextInt(5,21);
                              
                              // Convertir minutos a segundos
                              long tiempoEsperaMS = tiempoEstimado * 1000L;
                              
                              // Crear y enviar propuesta
                              ACLMessage proposal = msg.createReply();
                              proposal.setPerformative(ACLMessage.PROPOSE);
                              proposal.setContent("Tiempo estimado: " + tiempoEstimado + " minutos");
                              myAgent.send(proposal);
                              
                              // Cambiar estado interno y en el DF
                              actualizarDisponibilidadDF(true);

                              addBehaviour(new WakerBehaviour(myAgent, tiempoEsperaMS) {
                                   @Override
                                   protected void onWake() {
                                        ACLMessage entrega = msg.createReply();
                                        entrega.setPerformative(ACLMessage.INFORM);
                                        entrega.setContent("Pedido listo. Entrega finalizada");
                                        myAgent.send(entrega);

                                        // Volver a estar disponible
                                        actualizarDisponibilidadDF(false);
                                   }
                              });
                         }
                    } else {
                         block();
                    }
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
