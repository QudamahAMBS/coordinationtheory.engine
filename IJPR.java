// Copyright (c) 2023. This is source code is not to be used without the author's authorisation.
// Author: Dr. Qudamah Quboa <qudamah.quboa@manchester.ac.uk>

package coordinationtheory.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

public class CoordinationTheoryEngine {

	static String bpmnExportfilepath = "bpmnOutput.xml";
	static String Ontologyfilepath = "Ontology.owl";
	static OWLOntologyManager man;
	static OWLOntology ont;
	static OWLDataFactory dataFactory;
	static String base;
	static IRI ontologyIRI;
	static OWLReasoner reasoner;
	static ArrayList<String> values = new ArrayList<String>();
	static HashMap<String, String> hashValues = new HashMap<String, String>();
	static List<OntExtract> ontoData = new ArrayList<OntExtract>();
	static String ontologyLoopUpClass = "CallForTender";
	static String[] ontologyLoopUpProperties = {"hasFlowDependency"};
	static String[] ontologyLoopUpProcesses = { "isTaskOf" };
	static String[] ontologyLookUpProcessesTaskOf = { "_LeadSupplier", "_Partner" };
	static String[] ontologyLookUpProcessesTaskOfTitles= {"CFT_Received","Draft_Received"};
	static int BPMNLaneWidth = 1600;
	static int BPMNLaneHeight = 600;

	public static void main(String[] args) {
		
		initialiseOntologyReader();
		extractOntologyData();
		exportBPMNModel();
	}

	static void initialiseOntologyReader() {

		try {
			man = OWLManager.createOWLOntologyManager();
			File file = new File(Ontologyfilepath);
			IRI ontologysource = IRI.create(file);
			ont = man.loadOntologyFromOntologyDocument(ontologysource);
			base = ont.getOntologyID().getOntologyIRI() + "#";
			dataFactory = man.getOWLDataFactory();
			StructuralReasonerFactory factory = new StructuralReasonerFactory();
			reasoner = factory.createReasoner(ont);
		} catch (OWLOntologyCreationException e) {
			System.out.println("Could not load ontology: " + e.getMessage());
		}
	}

	public static void extractOntologyData() {
		int index = 0;
		OWLClass cls = dataFactory.getOWLClass(IRI.create(base + ontologyLoopUpClass));
		Set<OWLClassExpression> clsExp = cls.getSubClasses(ont);
		clsExp.add(cls);
		for (OWLClassExpression clss : clsExp) {
			NodeSet<OWLNamedIndividual> nset = reasoner.getInstances(clss, true);
			Set<OWLNamedIndividual> indSet = nset.getFlattened();
			for (OWLNamedIndividual i : indSet) {
				for (int j = 0; j < ontologyLoopUpProperties.length; j++) {
					Set<OWLIndividual> Flow = i.getObjectPropertyValues(CreateObjPro(ontologyLoopUpProperties[j]), ont);
					String source_processRef = "";
					for (int k = 0; k < ontologyLoopUpProcesses.length; k++) {
						Set<OWLIndividual> temp = i.getObjectPropertyValues(CreateObjPro(ontologyLoopUpProcesses[k]),
								ont);
						if (temp.size() > 0) {
							source_processRef = getString(temp.toArray()[0].toString());
							break;
						}
					}
					for (OWLIndividual indd : Flow) {
						String target_processRef = "";
						for (int k = 0; k < ontologyLoopUpProcesses.length; k++) {
							Set<OWLIndividual> temp = indd
									.getObjectPropertyValues(CreateObjPro(ontologyLoopUpProcesses[k]), ont);
							if (temp.size() > 0) {
								target_processRef = getString(temp.toArray()[0].toString());
								break;
							}
						}
						ontoData.add(new OntExtract(getString(i.toString()), source_processRef,
								ontologyLoopUpProperties[j], getString(indd.toString()), target_processRef, false));
						index = recursiveLoop(indd, ++index, ontologyLoopUpProperties[j],i);
					}
					for (OWLIndividual indd : Flow) {
						index = recursiveLoop(indd, index, ontologyLoopUpProperties[j],i);
					}
				}
			}
		}
	}

	public static int recursiveLoop(OWLIndividual i, int index, String ObjPro,OWLIndividual OrgSubject) {
		if(!(i.toStringID().equals(OrgSubject.toStringID()))) {
		if (i.getObjectPropertyValues(CreateObjPro(ObjPro), ont).size() > 0) {
			String source_processRef = "";
			for (int k = 0; k < ontologyLoopUpProcesses.length; k++) {
				Set<OWLIndividual> temp = i.getObjectPropertyValues(CreateObjPro(ontologyLoopUpProcesses[k]), ont);
				if (temp.size() > 0) {
					source_processRef = getString(temp.toArray()[0].toString());
					break;
				}
			}
			Set<OWLIndividual> Flow = i.getObjectPropertyValues(CreateObjPro(ObjPro), ont);
			for (OWLIndividual indd : Flow) {
				String target_processRef = "";
				for (int k = 0; k < ontologyLoopUpProcesses.length; k++) {
					Set<OWLIndividual> temp = indd.getObjectPropertyValues(CreateObjPro(ontologyLoopUpProcesses[k]),
							ont);
					if (temp.size() > 0) {
						target_processRef = getString(temp.toArray()[0].toString());
						break;
					}
				}
				ontoData.add(new OntExtract(getString(i.toString()), source_processRef, ObjPro,
						getString(indd.toString()), target_processRef, false));
				index++;
			    if (indd.getObjectPropertyValues(CreateObjPro(ObjPro), ont).size() > 0)
					if(!(indd.toStringID().equals(OrgSubject.toStringID())))
						index = recursiveLoop(indd, index, ObjPro,i);
			}
		}
		}
		return index;
	}

	public static String getString(String s) {
		return s.substring(s.indexOf("#") + 1, s.indexOf(">"));
	}

	public static OWLObjectProperty CreateObjPro(String objProp) {
		return dataFactory.getOWLObjectProperty(IRI.create(base + objProp));
	}

	@SuppressWarnings("unchecked")
	public static void exportBPMNModel() {
		List<String> temp = new ArrayList<String>();
		List<OntExtract> filtered = new ArrayList<OntExtract>();
		for (int i = 0; i < ontoData.size(); i++) {
			String search = ontoData.get(i).getString();
			if (!temp.contains(search)) {
				{
					temp.add(search);
					filtered.add(ontoData.get(i));
				}
			}
		}

		int EndTargetindex = -1;
		List<Integer> tempEndEventIndices = new ArrayList<Integer>();
		for (int i = 0; i < filtered.size(); i++) {
			String search = filtered.get(i).Object;
			String searchProcess=filtered.get(i).ObjectProcessRef;
			for (int j = 0; j < filtered.size(); j++) {
				if (filtered.get(j).Subject.contains(search)&&filtered.get(j).ObjectProcessRef.contains(searchProcess)) {
					EndTargetindex = -1;
					break;
				} else
					EndTargetindex = i;
			}
			if ((EndTargetindex != -1) && (!tempEndEventIndices.contains(EndTargetindex))) {
				tempEndEventIndices.add(EndTargetindex);
				EndTargetindex = -1;
			}
		}

		ontoData = filtered;

		try {

			List<Element> BPMNprocess = new ArrayList<Element>();
			List<Element> BPMNprocessLanes = new ArrayList<Element>();

			Document document = DocumentHelper.createDocument();
			Namespace bpmn = new Namespace("bpmn", "http://www.omg.org/spec/BPMN/20100524/MODEL");
			Namespace bpmndi = new Namespace("bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI");
			Namespace dc = new Namespace("dc", "http://www.omg.org/spec/DD/20100524/DC");
			Namespace di = new Namespace("di", "http://www.omg.org/spec/DD/20100524/DI");
			Element root = document.addElement(new QName("definitions", bpmn))
					.addAttribute("xmlns:bpmn", "http://www.omg.org/spec/BPMN/20100524/MODEL")
					.addAttribute("xmlns:bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI")
					.addAttribute("xmlns:di", "http://www.omg.org/spec/BPMN/20100524/DI")
					.addAttribute("xmlns:dc", "http://www.omg.org/spec/DD/20100524/DC")
					.addAttribute("xmlns:camunda", "http://camunda.org/schema/1.0/bpmn")
					.addAttribute("id", "Definitions_1vpr5nk")
					.addAttribute("targetNamespace", "http://bpmn.io/schema/bpmn")
					.addAttribute("exporter", "Camunda Modeler").addAttribute("exporterVersion", "2.2.3");
			Element Collaboration = root.addElement("bpmn:collaboration").addAttribute("id", "Collaboration_X");

			Element bpmnDiagram = root.addElement(new QName("BPMNDiagram", bpmndi)).addAttribute("id", "BPMNDiagram");
			Element bpmnPlane = bpmnDiagram.addElement("bpmndi:BPMNPlane").addAttribute("id", "BPMNPlane1")
					.addAttribute("bpmnElement", "Collaboration_X");

			for (int processIndex = 0; processIndex < ontologyLookUpProcessesTaskOf.length; processIndex++) {
				Collaboration.addElement("bpmn:participant").addAttribute("id", "Participant_process" + processIndex)
						.addAttribute("name", ontologyLookUpProcessesTaskOf[processIndex])
						.addAttribute("processRef", "process" + processIndex);
				Element elementtemp = bpmnPlane.addElement("bpmndi:BPMNShape")
						.addAttribute("id", "Participant_process" + processIndex + "di")
						.addAttribute("bpmnElement", "Participant_process" + processIndex)
						.addAttribute("isHorizontal", "true");
				elementtemp.addElement(new QName("Bounds", dc)).addAttribute("x", "150")
						.addAttribute("y", (50 + (processIndex * BPMNLaneHeight)) + "")
						.addAttribute("width", BPMNLaneWidth + "").addAttribute("height", BPMNLaneHeight + "");
				BPMNprocess.add(root.addElement("bpmn:process").addAttribute("id", "process" + processIndex)
						.addAttribute("isExecutable", "true"));

				BPMNprocessLanes.add(BPMNprocess.get(processIndex).addElement("bpmn:laneSet")
						.addAttribute("id", "processlaneSet" + processIndex).addElement("bpmn:Lane")
						.addAttribute("id", "processlaneSet" + processIndex + "Lane"));
			}

			BPMNObject[] bpmnObjArray = new BPMNObject[1000];
			BPMNObject[][] bpmnProcessArray = new BPMNObject[2][1000];
			int[] PTaskIndexcounter = new int[2];
			int tagsCounter = 0; 
			for (int i = 0; i < ontoData.size(); i++) {
				if (ontoData.get(i).Property == ontologyLoopUpProperties[0]) {
					int PSindex = -1, POindex = -1;
					if (ontoData.get(i).SubjectProcessRef.contains(ontologyLookUpProcessesTaskOf[0]))
						PSindex = 0;
					else if (ontoData.get(i).SubjectProcessRef.contains(ontologyLookUpProcessesTaskOf[1]))
						PSindex = 1;
					if (ontoData.get(i).ObjectProcessRef.contains(ontologyLookUpProcessesTaskOf[0]))
						POindex = 0;
					else if (ontoData.get(i).ObjectProcessRef.contains(ontologyLookUpProcessesTaskOf[1]))
						POindex = 1;
					if (tagsCounter == 0) {

						BPMNprocessLanes.get(POindex).addElement("bpmn:flowNodeRef").addText(ontoData.get(i).Object);

						bpmnObjArray[tagsCounter] = new BPMNObject(ontoData.get(i).Subject, 170,
								100 + BPMNLaneHeight * PSindex, 100, 80, 1, 0, ontoData.get(i).SubProcessStatus,
								ontoData.get(i).SubjectProcessRef);

						bpmnProcessArray[PSindex][PTaskIndexcounter[PSindex]] = bpmnObjArray[tagsCounter++];
						PTaskIndexcounter[PSindex] = PTaskIndexcounter[PSindex] + 1;

						bpmnObjArray[tagsCounter] = new BPMNObject(ontoData.get(i).Object, 400,
								100 + BPMNLaneHeight * POindex, 100, 80, 0, 1, ontoData.get(i).SubProcessStatus,
								ontoData.get(i).ObjectProcessRef);
						bpmnProcessArray[POindex][PTaskIndexcounter[POindex]] = bpmnObjArray[tagsCounter++];
						PTaskIndexcounter[POindex] = PTaskIndexcounter[POindex] + 1;

					} else {
						int tagsCounter2 = -1, PtagsCounter2 = -1;
						int tagsCounter3 = -1, PtagsCounter3 = -1;
						for (int j = 0; j < tagsCounter; j++) {

							if (bpmnObjArray[j].eventName.equals(ontoData.get(i).Subject)) {
								tagsCounter2 = j;
								for (int k = 0; k < bpmnProcessArray[PSindex].length; k++)
									if (bpmnProcessArray[PSindex][k] != null
											&& bpmnProcessArray[PSindex][k].eventName.equals(ontoData.get(i).Subject))
										PtagsCounter2 = k;

							}
						}
						if (PtagsCounter2 != -1) {
							bpmnObjArray[tagsCounter2].increaseFlow();
							bpmnProcessArray[PSindex][PtagsCounter2].increaseFlow();
						} else {
							boolean tagexist = false;
							for (int tempindex = 0; tempindex < tagsCounter; tempindex++)
								if (bpmnObjArray[tempindex].eventName == ontoData.get(i).Subject) {
									tagexist = true;
									break;
								}
							if (!tagexist) {
								BPMNprocessLanes.get(PSindex).addElement("bpmn:flowNodeRef")
										.addText(ontoData.get(i).Subject);
								bpmnObjArray[tagsCounter] = new BPMNObject(ontoData.get(i).Subject,
										bpmnProcessArray[PSindex][PTaskIndexcounter[PSindex] - 1].x,
										(bpmnProcessArray[PSindex][PTaskIndexcounter[PSindex] - 1].y + 100), 100, 80, 1,
										0, ontoData.get(i).SubProcessStatus, ontoData.get(i).SubjectProcessRef);
								bpmnProcessArray[PSindex][PTaskIndexcounter[PSindex]] = bpmnObjArray[tagsCounter++];
								PTaskIndexcounter[PSindex] = PTaskIndexcounter[PSindex] + 1;
							}
						}
						for (int j = 0; j < tagsCounter; j++) {
							if (bpmnObjArray[j].eventName.equals(ontoData.get(i).Object)) {
								tagsCounter3 = j;
								for (int k = 0; k < bpmnProcessArray[POindex].length; k++)
									if (bpmnProcessArray[POindex][k] != null
											&& bpmnProcessArray[POindex][k].eventName.equals(ontoData.get(i).Object))
										PtagsCounter3 = k;
							}
						}
						if (PtagsCounter3 != -1) {
							bpmnObjArray[tagsCounter3].inReFlow();
							bpmnProcessArray[POindex][PtagsCounter3].inReFlow();
						} else {
							BPMNprocessLanes.get(POindex).addElement("bpmn:flowNodeRef")
									.addText(ontoData.get(i).Object);
							if (PtagsCounter2 != -1 && POindex == PSindex) {
								boolean tagexist = false;
								for (int tempindex = 0; tempindex < tagsCounter; tempindex++)
									if (bpmnObjArray[tempindex].eventName == ontoData.get(i).Object) {
										tagexist = true;
										break;
									}
								if (!tagexist) {
									bpmnObjArray[tagsCounter] = new BPMNObject(ontoData.get(i).Object,
											bpmnObjArray[tagsCounter2].x + 230,
											bpmnObjArray[tagsCounter2].y
													+ 50 * (bpmnProcessArray[POindex][PtagsCounter2].NoFlow - 2),
											100, 80, 0, 1, ontoData.get(i).SubProcessStatus,
											ontoData.get(i).ObjectProcessRef);
									bpmnProcessArray[POindex][PTaskIndexcounter[POindex]] = bpmnObjArray[tagsCounter++];
									PTaskIndexcounter[POindex] = PTaskIndexcounter[POindex] + 1;
								}
							} else {

								if (PTaskIndexcounter[POindex] == 0) {
									
									bpmnObjArray[tagsCounter] = new BPMNObject(ontoData.get(i).Object, 400,
											100 + BPMNLaneHeight * POindex, 100, 80, 0, 1,
											ontoData.get(i).SubProcessStatus, ontoData.get(i).ObjectProcessRef);
									bpmnProcessArray[POindex][PTaskIndexcounter[POindex]] = bpmnObjArray[tagsCounter++];
									PTaskIndexcounter[POindex] = PTaskIndexcounter[POindex] + 1;
								}

								else {
									boolean tagexist = false;
									for (int tempindex = 0; tempindex < tagsCounter; tempindex++)
										if (bpmnObjArray[tempindex].eventName == ontoData.get(i).Object) {
											tagexist = true;
											break;
										}
									if (!tagexist) {
										bpmnObjArray[tagsCounter] = new BPMNObject(ontoData.get(i).Object,
												bpmnProcessArray[POindex][PTaskIndexcounter[POindex] - 1].x + 230,
												bpmnProcessArray[POindex][PTaskIndexcounter[POindex] - 1].y, 100, 80, 0,
												1, ontoData.get(i).SubProcessStatus, ontoData.get(i).ObjectProcessRef);
										bpmnProcessArray[POindex][PTaskIndexcounter[POindex]] = bpmnObjArray[tagsCounter++];
										PTaskIndexcounter[POindex] = PTaskIndexcounter[POindex] + 1;
									}
								}
							}
						}
					}
				}

			}

			List<Integer> EndEventIndices = new ArrayList<Integer>();
			for (int i = 0; i < tempEndEventIndices.size(); i++)
				for (int j = 0; j < tagsCounter; j++) {
					if (bpmnObjArray[j].eventName.contains(ontoData.get(tempEndEventIndices.get(i)).Object)) {
						if (!EndEventIndices.contains(j))
							EndEventIndices.add(j);
						break;
					}
				}

			Element e1;
			for(int i=0;i<ontologyLookUpProcessesTaskOf.length;i++)
			if (tagsCounter > 0) {
				BPMNprocessLanes.get(i).addElement("bpmn:flowNodeRef").addText(ontologyLookUpProcessesTaskOfTitles[i]);
				BPMNprocess.get(i).addElement("bpmn:startEvent").addAttribute("id", ontologyLookUpProcessesTaskOfTitles[i]).addAttribute("name",
						ontologyLookUpProcessesTaskOfTitles[i]);
				e1 = bpmnPlane.addElement("bpmndi:BPMNShape").addAttribute("id", ontologyLookUpProcessesTaskOfTitles[i]+"Shape")
						.addAttribute("bpmnElement", ontologyLookUpProcessesTaskOfTitles[i]);
				e1.addElement(new QName("Bounds", dc)).addAttribute("x", "233").addAttribute("y", (123+(i * BPMNLaneHeight))+"")
						.addAttribute("width", "36").addAttribute("height", "36");
				Element BPMNlable1 = e1.addElement("bpmndi:BPMNLabel");
				BPMNlable1.addElement(new QName("Bounds", dc)).addAttribute("x", "218").addAttribute("y", (163+(i * BPMNLaneHeight))+"")
						.addAttribute("width", "100").addAttribute("height", "15");

			}
			for (int i = 1; i < tagsCounter; i++) {
				int PRindex = -1;
				if (bpmnObjArray[i].processRef.contains(ontologyLookUpProcessesTaskOf[0]))
					PRindex = 0;
				else if (bpmnObjArray[i].processRef.contains(ontologyLookUpProcessesTaskOf[1]))
					PRindex = 1;
				BPMNprocess.get(PRindex).addElement("bpmn:serviceTask").addAttribute("id", bpmnObjArray[i].eventName)
						.addAttribute("name", bpmnObjArray[i].eventName).addAttribute("camunda:asyncBefore", "true")
						.addAttribute("camunda:delegateExpression", "#{sysoutAdapter}");
				e1 = bpmnPlane.addElement("bpmndi:BPMNShape").addAttribute("id", bpmnObjArray[i].eventName + "_123")
						.addAttribute("bpmnElement", bpmnObjArray[i].eventName);
				e1.addElement(new QName("Bounds", dc)).addAttribute("x", Integer.toString(bpmnObjArray[i].x))
						.addAttribute("y", Integer.toString(bpmnObjArray[i].y))
						.addAttribute("width", Integer.toString(bpmnObjArray[i].width))
						.addAttribute("height", Integer.toString(bpmnObjArray[i].height));
			}

			if (tagsCounter > 0) {
				for (int i = 0; i < EndEventIndices.size(); i++) {
					int PRindex = -1;
					if (bpmnObjArray[EndEventIndices.get(i)].processRef.contains(ontologyLookUpProcessesTaskOf[0]))
						PRindex = 0;
					else if (bpmnObjArray[EndEventIndices.get(i)].processRef.contains(ontologyLookUpProcessesTaskOf[1]))
						PRindex = 1;

					BPMNprocess.get(PRindex).addElement("bpmn:endEvent").addAttribute("id", "endEvent" + (i + 1))
							.addAttribute("name", "End of " + bpmnObjArray[EndEventIndices.get(i)].eventName);
					BPMNprocessLanes.get(PRindex).addElement("bpmn:flowNodeRef").addText("endEvent" + (i + 1));

					e1 = bpmnPlane.addElement("bpmndi:BPMNShape").addAttribute("id", "endEventShape" + (i + 1))
							.addAttribute("bpmnElement", "endEvent" + (i + 1));
					e1.addElement(new QName("Bounds", dc))
							.addAttribute("x", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].x + 250))
							.addAttribute("y", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].y + 19))
							.addAttribute("width", "36").addAttribute("height", "36");
					Element BPMNlabel = e1.addElement("bpmndi:BPMNLabel");
					BPMNlabel.addElement(new QName("Bounds", dc))
							.addAttribute("x", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].x + 210))
							.addAttribute("y", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].y + 60))
							.addAttribute("width", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].width))
							.addAttribute("height", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].height));
				}
			}

			for (int i = 0; i < tagsCounter; i++) { 
				int xdistance = 0;
				if (bpmnObjArray[i].NoFlow == 6) {
					xdistance = 135;
					int PRindex = -1;
					if (bpmnObjArray[i].processRef.contains(ontologyLookUpProcessesTaskOf[0]))
						PRindex = 0;
					else if (bpmnObjArray[i].processRef.contains(ontologyLookUpProcessesTaskOf[1]))
						PRindex = 1;
					BPMNprocessLanes.get(PRindex).addElement("bpmn:flowNodeRef")
							.addText(bpmnObjArray[i].eventName + "exclusiveGatewayNoFlow");
					BPMNprocess.get(PRindex).addElement("bpmn:exclusiveGateway").addAttribute("id",
							bpmnObjArray[i].eventName + "exclusiveGatewayNoFlow");
					e1 = bpmnPlane.addElement("bpmndi:BPMNShape")
							.addAttribute("id", bpmnObjArray[i].eventName + "exclusiveGateway_NoFlow")
							.addAttribute("bpmnElement", bpmnObjArray[i].eventName + "exclusiveGatewayNoFlow");
					e1.addElement(new QName("Bounds", dc))
							.addAttribute("x", Integer.toString(bpmnObjArray[i].x + xdistance))
							.addAttribute("y", Integer.toString(bpmnObjArray[i].y + 20)).addAttribute("width", "40")
							.addAttribute("height", "40");
				}

			}

			for (int i = 0; i < ontoData.size(); i++) {
				if (ontoData.get(i).Property == ontologyLoopUpProperties[0]) {
					int j = -1;
					int k = -1;
					for (int l = 0; l < tagsCounter; l++) {
						if (ontoData.get(i).Subject.equals(bpmnObjArray[l].eventName)
								&& ontoData.get(i).SubjectProcessRef.equals(bpmnObjArray[l].processRef)) {
							j = l;
						}
						if (ontoData.get(i).Object.equals(bpmnObjArray[l].eventName)
								&& ontoData.get(i).ObjectProcessRef.equals(bpmnObjArray[l].processRef)) {
							k = l;
						}
					}

					if (bpmnObjArray[j].NoFlow != 6) {
						String sourceRef, sourceProcRef;
						int xdistance, ydistance;
						if (j == 0) {
							sourceRef = ontologyLookUpProcessesTaskOfTitles[0];
							sourceProcRef = ontologyLookUpProcessesTaskOf[0];
							xdistance = 36;
							ydistance = 40;
						} else {
							sourceRef = bpmnObjArray[j].eventName;
							sourceProcRef = bpmnObjArray[j].processRef;

						}
						int PRindex = -1;

						if (bpmnObjArray[k].processRef.contains(sourceProcRef))
						{
							xdistance = 100;
							ydistance = 40;
							if (bpmnObjArray[k].processRef.contains(ontologyLookUpProcessesTaskOf[0]))
								PRindex = 0;
							else if (bpmnObjArray[k].processRef.contains(ontologyLookUpProcessesTaskOf[1]))
								PRindex = 1;

							BPMNprocess.get(PRindex).addElement("bpmn:sequenceFlow")
									.addAttribute("id", sourceRef + bpmnObjArray[k].eventName)
									.addAttribute("sourceRef", sourceRef)
									.addAttribute("targetRef", bpmnObjArray[k].eventName);
							e1 = bpmnPlane.addElement("bpmndi:BPMNEdge")
									.addAttribute("id", sourceRef + bpmnObjArray[k].eventName + "_123")
									.addAttribute("bpmnElement", sourceRef + bpmnObjArray[k].eventName);
							e1.addElement(new QName("waypoint", di))
									.addAttribute("x", Integer.toString(bpmnObjArray[j].x + xdistance))
									.addAttribute("y", Integer.toString(bpmnObjArray[j].y + ydistance));
							e1.addElement(new QName("waypoint", di))
									.addAttribute("x", Integer.toString(bpmnObjArray[k].x))
									.addAttribute("y", Integer.toString(bpmnObjArray[k].y + ydistance));
							List<Element> list = BPMNprocess.get(PRindex).elements();
							for (Element e : list) {
								if (e.getName().equals("serviceTask")) {
									Attribute nameAttr = e.attribute("id");
									if (nameAttr.getValue().equals(sourceRef)) {
										e.addElement("bpmn:outgoing").addText(sourceRef + bpmnObjArray[k].eventName);
									}
									if (nameAttr.getValue().equals(bpmnObjArray[k].eventName)) {
										e.addElement("bpmn:incoming").addText(sourceRef + bpmnObjArray[k].eventName);
									}
								} else {
									if (e.getName().equals("startEvent")) {
										Attribute nameAttr = e.attribute("id");
										if (nameAttr.getValue().equals(sourceRef)) {
											e.addElement("bpmn:outgoing")
													.addText(sourceRef + bpmnObjArray[k].eventName);
										}
									}

								}
							}
						} else { 
							xdistance = 50;
							ydistance = 80;
							if (bpmnObjArray[k].processRef.contains(ontologyLookUpProcessesTaskOf[0]))
								PRindex = 0;
							else if (bpmnObjArray[k].processRef.contains(ontologyLookUpProcessesTaskOf[1]))
								PRindex = 1;

							if (bpmnObjArray[j].y < bpmnObjArray[k].y) {
								Collaboration.addElement("bpmn:messageFlow")
										.addAttribute("id", sourceRef + bpmnObjArray[k].eventName)
										.addAttribute("sourceRef", sourceRef)
										.addAttribute("targetRef", bpmnObjArray[k].eventName);

								e1 = bpmnPlane.addElement("bpmndi:BPMNEdge")
										.addAttribute("id", sourceRef + bpmnObjArray[k].eventName + "_msgFlow")
										.addAttribute("bpmnElement", sourceRef + bpmnObjArray[k].eventName);

								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + xdistance))
										.addAttribute("y", Integer.toString(bpmnObjArray[j].y + ydistance));
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[k].x + xdistance))
										.addAttribute("y", Integer.toString(bpmnObjArray[k].y));
							} else if (bpmnObjArray[j].y > bpmnObjArray[k].y) {
								Collaboration.addElement("bpmn:messageFlow")
										.addAttribute("id", sourceRef + bpmnObjArray[k].eventName)
										.addAttribute("sourceRef", sourceRef)
										.addAttribute("targetRef", bpmnObjArray[k].eventName);

								e1 = bpmnPlane.addElement("bpmndi:BPMNEdge")
										.addAttribute("id", sourceRef + bpmnObjArray[k].eventName + "_msgFlow")
										.addAttribute("bpmnElement", sourceRef + bpmnObjArray[k].eventName);

								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + xdistance + 10))
										.addAttribute("y", Integer.toString(bpmnObjArray[j].y));
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[k].x + xdistance + 10))
										.addAttribute("y", Integer.toString(bpmnObjArray[k].y + ydistance));
							}
						}

					}

					if (bpmnObjArray[j].NoFlow == 6) {
						int PRindex = -1;
						if (bpmnObjArray[j].processRef.contains(ontologyLookUpProcessesTaskOf[0]))
							PRindex = 0;
						else if (bpmnObjArray[j].processRef.contains(ontologyLookUpProcessesTaskOf[1]))
							PRindex = 1;
						if (bpmnObjArray[j].processRef.equals(bpmnObjArray[k].processRef)) {
							int tagsCounter2 = 0;
							List<Element> list = BPMNprocess.get(PRindex).elements();
							for (Element e : list) {
								if (e.getName().equals("sequenceFlow")) {
									Attribute nameAttr = e.attribute("id");
									if (nameAttr.getValue().equals(bpmnObjArray[j].eventName + bpmnObjArray[j].eventName
											+ "exclusiveGatewayNoFlow")) {
										tagsCounter2 = 1;
									}
								}
							}

							BPMNprocess.get(PRindex).addElement("bpmn:sequenceFlow")
									.addAttribute("id",
											bpmnObjArray[j].eventName + "exclusiveGateway" + bpmnObjArray[k].eventName)
									.addAttribute("sourceRef", bpmnObjArray[j].eventName + "exclusiveGatewayNoFlow")
									.addAttribute("targetRef", bpmnObjArray[k].eventName);
							e1 = bpmnPlane.addElement("bpmndi:BPMNEdge")
									.addAttribute("id",
											bpmnObjArray[j].eventName + "exclusiveGateway" + bpmnObjArray[k].eventName
													+ "_NoFlow")
									.addAttribute("bpmnElement",
											bpmnObjArray[j].eventName + "exclusiveGateway" + bpmnObjArray[k].eventName);
							if (bpmnObjArray[k].y == bpmnObjArray[j].y) {
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + 175))
										.addAttribute("y", Integer.toString(bpmnObjArray[j].y + 40));
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[k].x))
										.addAttribute("y", Integer.toString(bpmnObjArray[k].y + 40));
							} else {

								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + 155))
										.addAttribute("y", Integer.toString(bpmnObjArray[j].y + 60));
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + 155))
										.addAttribute("y", Integer.toString(bpmnObjArray[k].y + 40));
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[k].x))
										.addAttribute("y", Integer.toString(bpmnObjArray[k].y + 40));
							}
							for (Element e : list) {
								if (e.getName().equals("exclusiveGateway")) {
									Attribute nameAttr = e.attribute("id");
									if (nameAttr.getValue()
											.equals(bpmnObjArray[j].eventName + "exclusiveGatewayNoFlow")) {
										e.addElement("bpmn:outgoing").addText(bpmnObjArray[j].eventName
												+ "exclusiveGateway" + bpmnObjArray[k].eventName);
									}
								}
								if (e.getName().equals("serviceTask")) {
									Attribute nameAttr = e.attribute("id");
									if (nameAttr.getValue().equals(bpmnObjArray[k].eventName)) {
										e.addElement("bpmn:incoming").addText(bpmnObjArray[j].eventName
												+ "exclusiveGateway" + bpmnObjArray[k].eventName);
									}
								}

							}
							if (tagsCounter2 == 0) {
								int PRindex2 = -1;
								if (bpmnObjArray[j].processRef.contains(ontologyLookUpProcessesTaskOf[0]))
									PRindex2 = 0;
								else if (bpmnObjArray[j].processRef.contains(ontologyLookUpProcessesTaskOf[1]))
									PRindex2 = 1;
								BPMNprocess.get(PRindex2).addElement("bpmn:sequenceFlow")
										.addAttribute("id",
												bpmnObjArray[j].eventName + bpmnObjArray[j].eventName
														+ "exclusiveGatewayNoFlow")
										.addAttribute("sourceRef", bpmnObjArray[j].eventName).addAttribute("targetRef",
												bpmnObjArray[j].eventName + "exclusiveGatewayNoFlow");
								e1 = bpmnPlane.addElement("bpmndi:BPMNEdge")
										.addAttribute("id",
												bpmnObjArray[j].eventName + bpmnObjArray[j].eventName
														+ "exclusiveGateway_NoFlow")
										.addAttribute("bpmnElement", bpmnObjArray[j].eventName
												+ bpmnObjArray[j].eventName + "exclusiveGatewayNoFlow");
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + 100))
										.addAttribute("y", Integer.toString(bpmnObjArray[j].y + 40));
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + 135))
										.addAttribute("y", Integer.toString(bpmnObjArray[j].y + 40));
								for (Element e : list) {
									if (e.getName().equals("exclusiveGateway")) {
										Attribute nameAttr = e.attribute("id");
										if (nameAttr.getValue()
												.equals(bpmnObjArray[j].eventName + "exclusiveGatewayNoFlow")) {
											e.addElement("bpmn:incoming").addText(bpmnObjArray[j].eventName
													+ bpmnObjArray[j].eventName + "exclusiveGatewayNoFlow");
										}
									}
									if (e.getName().equals("serviceTask")) {
										Attribute nameAttr = e.attribute("id");
										if (nameAttr.getValue().equals(bpmnObjArray[j].eventName)) {
											e.addElement("bpmn:outgoing").addText(bpmnObjArray[j].eventName
													+ bpmnObjArray[j].eventName + "exclusiveGatewayNoFlow");
										}
									}

								}
							}
						} else {
							int xdistance = 50;
							int ydistance = 80;
							String sourceRef = bpmnObjArray[j].eventName;
							if (bpmnObjArray[j].y < bpmnObjArray[k].y) {

								Collaboration.addElement("bpmn:messageFlow")
										.addAttribute("id", sourceRef + bpmnObjArray[k].eventName)
										.addAttribute("sourceRef", sourceRef)
										.addAttribute("targetRef", bpmnObjArray[k].eventName);

								e1 = bpmnPlane.addElement("bpmndi:BPMNEdge")
										.addAttribute("id", sourceRef + bpmnObjArray[k].eventName + "_msgFlow")
										.addAttribute("bpmnElement", sourceRef + bpmnObjArray[k].eventName);

								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + xdistance))
										.addAttribute("y", Integer.toString(bpmnObjArray[j].y + ydistance));
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[k].x + xdistance))
										.addAttribute("y", Integer.toString(bpmnObjArray[k].y));
							} else if (bpmnObjArray[j].y > bpmnObjArray[k].y) {
								Collaboration.addElement("bpmn:messageFlow")
										.addAttribute("id", sourceRef + bpmnObjArray[k].eventName)
										.addAttribute("sourceRef", sourceRef)
										.addAttribute("targetRef", bpmnObjArray[k].eventName);

								e1 = bpmnPlane.addElement("bpmndi:BPMNEdge")
										.addAttribute("id", sourceRef + bpmnObjArray[k].eventName + "_msgFlow")
										.addAttribute("bpmnElement", sourceRef + bpmnObjArray[k].eventName);

								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[j].x + xdistance + 10))
										.addAttribute("y", Integer.toString(bpmnObjArray[j].y));
								e1.addElement(new QName("waypoint", di))
										.addAttribute("x", Integer.toString(bpmnObjArray[k].x + xdistance + 10))
										.addAttribute("y", Integer.toString(bpmnObjArray[k].y + ydistance));
							}
						}

					}

				}
			}

			if (tagsCounter > 0) {
				for (int i = 0; i < EndEventIndices.size(); i++) {
					String sourceRef = bpmnObjArray[EndEventIndices.get(i)].eventName;
					String targetRef = "endEvent" + (i + 1);
					int PRindex = -1;
					if (bpmnObjArray[EndEventIndices.get(i)].processRef.contains(ontologyLookUpProcessesTaskOf[0]))
						PRindex = 0;
					else if (bpmnObjArray[EndEventIndices.get(i)].processRef.contains(ontologyLookUpProcessesTaskOf[1]))
						PRindex = 1;
					BPMNprocess.get(PRindex).addElement("bpmn:sequenceFlow").addAttribute("id", sourceRef + targetRef)
							.addAttribute("sourceRef", sourceRef).addAttribute("targetRef", targetRef);
					e1 = bpmnPlane.addElement("bpmndi:BPMNEdge").addAttribute("id", sourceRef + targetRef + "_edge")
							.addAttribute("bpmnElement", sourceRef + targetRef);
					e1.addElement(new QName("waypoint", di))
							.addAttribute("x", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].x + 100))
							.addAttribute("y", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].y + 40));
					e1.addElement(new QName("waypoint", di))
							.addAttribute("x", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].x + 250))
							.addAttribute("y", Integer.toString(bpmnObjArray[EndEventIndices.get(i)].y + 40));
					List<Element> list = BPMNprocess.get(PRindex).elements();
					for (Element e : list) {
						if (e.getName().equals("endEvent")) {
							Attribute nameAttr = e.attribute("id");
							if (nameAttr.getValue().equals(targetRef)) {
								e.addElement("bpmn:incoming").addText(sourceRef + targetRef);
							}
						}
						if (e.getName().equals("serviceTask")) {
							Attribute nameAttr = e.attribute("id");
							if (nameAttr.getValue().equals(sourceRef)) {
								e.addElement("bpmn:outgoing").addText(sourceRef + targetRef);
							}
						}
					}
				}
			}

			for(int i=1;i<ontologyLookUpProcessesTaskOf.length;i++)
			{
				String tempid=ontologyLookUpProcessesTaskOfTitles[i] + bpmnProcessArray[i][0].eventName;
				List<Element> list = BPMNprocess.get(i).elements();
				for (Element e : list) {
					if (e.getName().equals("startEvent")) {
						Attribute nameAttr = e.attribute("id");
						if (nameAttr.getValue().equals(ontologyLookUpProcessesTaskOfTitles[i])) {
							e.addElement("bpmn:outgoing")
									.addText(tempid);
						}
					}
					else if (e.getName().equals("serviceTask")) {
						Attribute nameAttr = e.attribute("id");
					if (nameAttr.getValue().equals(bpmnProcessArray[i][0].eventName)) {
						e.addElement("bpmn:incoming").addText(tempid);
						}
					} 
					}
					
				BPMNprocess.get(i).addElement("bpmn:sequenceFlow")
					.addAttribute("id", tempid)
					.addAttribute("sourceRef", ontologyLookUpProcessesTaskOfTitles[i])
					.addAttribute("targetRef",bpmnProcessArray[i][0].eventName);
			e1 = bpmnPlane.addElement("bpmndi:BPMNEdge")
					.addAttribute("id", tempid + "_123")
					.addAttribute("bpmnElement", tempid);
			e1.addElement(new QName("waypoint", di))
					.addAttribute("x", Integer.toString((233+36)))
					.addAttribute("y", Integer.toString((123+(i * BPMNLaneHeight)) + 20));
			e1.addElement(new QName("waypoint", di))
					.addAttribute("x", Integer.toString(bpmnProcessArray[i][0].x))
					.addAttribute("y", Integer.toString(bpmnProcessArray[i][0].y + 40));
						}
					
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter(new FileOutputStream(bpmnExportfilepath), format);
			writer.write(document);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class OntExtract {
	public String Subject;
	public String SubjectProcessRef;
	public String Property;
	public String Object;
	public String ObjectProcessRef;
	public boolean SubProcessStatus;

	public OntExtract(String Subject, String SubjectProcessRef, String Property, String Object, String ObjectProcessRef,
			boolean SubProcessStatus) {
		this.Subject = Subject;
		this.SubjectProcessRef = SubjectProcessRef;
		this.Property = Property;
		this.Object = Object;
		this.ObjectProcessRef = ObjectProcessRef;
		this.SubProcessStatus = SubProcessStatus;
	}

	public String getString() {
		return Subject + " " + Property + " " + Object + " " + SubProcessStatus;
	}
}

class BPMNObject {
	public String eventName;
	public int x;
	public int y;
	public int width;
	public int height;
	public int NoFlow;
	public int ReFlow;
	public boolean SubProcessStatus;
	public String processRef;

	public BPMNObject(String eventName, int x, int y, int width, int height, int NoFlow, int ReFlow,
			boolean SubProcessStatus, String processRef) {
		this.eventName = eventName;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.NoFlow = NoFlow;
		this.ReFlow = ReFlow;
		this.SubProcessStatus = SubProcessStatus;
		this.processRef = processRef;
	}

	public void increaseFlow() {
		NoFlow++;
	}

	public void inReFlow() {
		ReFlow++;
	}
}