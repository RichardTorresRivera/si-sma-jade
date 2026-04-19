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
                         ACLMessage reply = msg.createReply();

                         if (ocupado) {
                              reply.setPerformative(ACLMessage.REFUSE);
                              reply.setContent("Lo siento, estoy ocupado con otra entrega");
                         } else {
                              reply.setPerformative(ACLMessage.PROPOSE);
                              
                              // Tiempo estimado aleatorio
                              Random rand = new Random();
                              int tiempoEstimado = rand.nextInt(5,21);

                              reply.setContent("Tiempo estimado: " + tiempoEstimado + "minutos");
                         }
                         
                         myAgent.send(reply);
                    } else {
                         block();
                    }
               }
          });
     }

}
