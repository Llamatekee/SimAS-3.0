package gramatica;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.draw.LineSeparator;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import simulador.SimulacionFinal.HistorialPaso;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.Collections;


public class Gramatica {

    // Propiedades para permitir el binding con la UI en JavaFX
    private StringProperty nombre = new SimpleStringProperty();
    private StringProperty descripcion = new SimpleStringProperty();
    private StringProperty simbInicial = new SimpleStringProperty();
    private final IntegerProperty estado = new SimpleIntegerProperty();

    // Colecciones de objetos de la gramática (modelo)
    private final ObservableList<Terminal> terminales = FXCollections.observableArrayList();
    private final ObservableList<NoTerminal> noTerminales = FXCollections.observableArrayList();
    private ObservableList<Produccion> pr = FXCollections.observableArrayList();

    // Colecciones para representar los modelos de las listas en la UI (por ejemplo, nombres de símbolos)
    private ObservableList<String> noTerm = FXCollections.observableArrayList();
    private ObservableList<String> term = FXCollections.observableArrayList();
    private final ObservableList<String> producciones = FXCollections.observableArrayList();

    private TablaPredictiva tpredictiva = new TablaPredictiva();
    //private NuevaDerivacionDescGenerada derivacionGeneradaDesc;

    // Constructor con parámetros
    public Gramatica(String nombre, String descripcion) {
        this.nombre.set(nombre);
        this.descripcion.set(descripcion);
    }

    // Constructor con parámetros
    public Gramatica(Gramatica gramatica) {
        this.nombre.set(gramatica.getNombre());
        this.descripcion.set(gramatica.getDescripcion());
        this.estado.set(gramatica.getEstado());
        this.noTerm.setAll(gramatica.getNoTerminalesModel());
        this.term.setAll(gramatica.getTerminalesModel());
        this.producciones.setAll(gramatica.getProduccionesModel());
        this.simbInicial.set(gramatica.getSimbInicial());
        this.noTerminales.setAll(gramatica.getNoTerminales());
        this.terminales.setAll(gramatica.getTerminales());
        this.pr.setAll(gramatica.getProducciones());
    }

    // Constructor sin parámetros
    public Gramatica() {
        // En la versión Swing se llamaba a initComponents() para inicializar la UI,
        // pero aquí la clase Gramatica es parte del modelo, por lo que no se requiere.
    }


    public void actualizarNoTerminalesDesdeModel() {
        // Crear un mapa para mantener las referencias originales de los NoTerminales
        Map<String, NoTerminal> mapaNoTerminales = this.noTerminales.stream()
                .collect(Collectors.toMap(NoTerminal::getNombre, nt -> nt));

        // Limpiar la lista de objetos NoTerminal
        this.noTerminales.clear();

        // Actualizar la lista de objetos NoTerminal a partir de la lista de nombres
        for (String nombre : this.noTerm) {
            NoTerminal nt = mapaNoTerminales.get(nombre);
            if (nt == null) {
                nt = new NoTerminal(nombre, nombre);
            }
            this.noTerminales.add(nt);
        }
    }

    // Getters y setters usando las propiedades

    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public void setDescripcion(String descripcion) {
        this.descripcion.set(descripcion);
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public int getEstado() {
        return estado.get();
    }

    public void setEstado(int estado) {
        this.estado.set(estado);
    }

    public IntegerProperty estadoProperty() {
        return estado;
    }

    public void setVocabulario(ObservableList<String> noTerm, ObservableList<String> term) {
        // Limpiar las listas existentes tanto de los modelos UI como de los datos del modelo
        this.noTerm.clear();
        this.noTerminales.clear();
        this.term.clear();
        this.terminales.clear();

        // Actualizar la lista de NoTerminales a partir de la lista de cadenas 'noTerm'
        if (noTerm != null) {
            for (String s : noTerm) {
                NoTerminal noterminal = new NoTerminal(s, s);
                this.noTerminales.add(noterminal);
            }
            // También se guarda la lista de strings para la UI (binding a ListView, por ejemplo)
            this.noTerm.addAll(noTerm);
        }

        // Actualizar la lista de Terminales a partir de la lista de cadenas 'term'
        if (term != null) {
            for (String s : term) {
                Terminal terminal = new Terminal(s, s);
                this.terminales.add(terminal);
            }
            // También se guarda la lista de strings para la UI
            this.term.addAll(term);
        }
    }

    public void numerarProducciones() {
        int index = 1;
        for (Produccion produccion : pr) {
            produccion.setNumero(index++);
        }
    }
    public int getNumeroProduccion(String produccion) {
        for (Produccion pr : this.getProducciones()) {
            if (pr.toString().equals(produccion)) { // 🔥 Comparar exacto, no `contains`
                return pr.getNumero();
            }
        }
        return -1; // No encontrada
    }

    // Métodos para terminales

    public ObservableList<Terminal> getTerminales() {
        return terminales;
    }

    public void setTerminales(ObservableList<Terminal> terminales) {
        if (!this.terminales.equals(terminales)) {
            this.terminales.setAll(terminales);
        }
        this.term.setAll(terminales.stream().map(Terminal::getNombre).collect(Collectors.toList()));
    }

    public ObservableList<String> getTerminalesModel() {
        return term;
    }

    public void setTerminalesModel(ObservableList<String> term) {
        this.term.setAll(term);
        this.terminales.setAll(term.stream().map(s -> new Terminal(s, s)).collect(Collectors.toList()));
    }

    // Métodos para no terminales

    public ObservableList<NoTerminal> getNoTerminales() {
        return noTerminales;
    }

    public void setNoTerminales(ObservableList<NoTerminal> noTerminales) {
        if (!this.noTerminales.equals(noTerminales)) {
            this.noTerminales.setAll(noTerminales);
        }
        this.noTerm.setAll(noTerminales.stream().map(NoTerminal::getNombre).collect(Collectors.toList()));
    }

    public ObservableList<String> getNoTerminalesModel() {
        return noTerm;
    }

    public void setNoTerminalesModel(ObservableList<String> noTerminal) {
        if (!this.noTerm.equals(noTerminal)) {
            this.noTerm.setAll(noTerminal);
        }
        actualizarNoTerminalesDesdeModel();
    }

    // Métodos para producciones

    public ObservableList<Produccion> getProducciones() {
        return pr;
    }

    public void setProducciones(ObservableList<Produccion> pr) {
        if (!this.pr.equals(pr)) {
            this.pr.setAll(pr);
        }
        this.numerarProducciones();
        this.producciones.setAll(pr.stream().map(Produccion::toString).collect(Collectors.toList()));
    }

    public ObservableList<String> getProduccionesModel() {
        return producciones;
    }

    public void setProduccionesModel(ObservableList<String> producciones) {
        this.producciones.setAll(producciones);
        this.pr.setAll(producciones.stream().map(this::crearProduccionDesdeString).collect(Collectors.toList()));
        this.numerarProducciones();
    }

    private Produccion crearProduccionDesdeString(String produccionStr) {
        String[] partes = produccionStr.split("→");
        if (partes.length < 2) return null;

        String antecedente = partes[0].trim();
        String[] consecuente = partes[1].trim().split(" ");

        Antecedente antec = new Antecedente();
        NoTerminal nt = new NoTerminal(antecedente, antecedente);
        antec.setSimboloNT(nt);

        ObservableList<Simbolo> consec = FXCollections.observableArrayList();
        for (String s : consecuente) {
            consec.add(new Simbolo(s, s));
        }

        Produccion produccion = new Produccion();
        produccion.setAntec(antec);
        produccion.setConsec(consec);
        return produccion;
    }

    // Setter usando la propiedad de JavaFX
    public void setSimbInicial(String simInicial) {
        this.simbInicial.set(simInicial);
    }

    // Getter usando la propiedad de JavaFX
    public String getSimbInicial() {
        return this.simbInicial.get();
    }

    // Método adicional para exponer la propiedad, útil para binding
    public StringProperty simbInicialProperty() {
        return this.simbInicial;
    }

    /*public NuevaDerivacionDescGenerada getDerivacionGeneradaDesc() {
        return derivacionGeneradaDesc;
    }*/

    /*public void setDerivacionGeneradaDesc(NuevaDerivacionDescGenerada derivacionGeneradaDesc) {
        this.derivacionGeneradaDesc = derivacionGeneradaDesc;
    }*/

    public void selecSimboloInicial(String simInicial) {
        for (NoTerminal nt : this.noTerminales) {
            if (nt.toString().equals(simInicial)) {
                nt.setSimboloInicial(true);
                break;
            }
        }
    }


    public int guardarGramatica(Window ownerWindow) {
        // Crear y configurar el FileChooser de JavaFX
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de XML", "*.xml"));
        fileChooser.setTitle("Guardar");

        // Mostrar el diálogo de guardado (se le pasa el owner window, puede ser null si no se dispone)
        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file == null) {
            // El usuario canceló la operación
            return -2;
        }

        // Construir el documento XML
        String documentoXml = "";

        // Cabecera XML
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<?xml-stylesheet type=\"text/xsl\" href=\"gramatica.xsl\"?>\n");
        sb.append("<grammar version=\"2.0\">\n");
        // Nombre y descripción
        sb.append("\t<name>").append(this.getNombre()).append("</name>\n");
        sb.append("\t<description>").append(this.getDescripcion()).append("</description>\n");

        // Sección de símbolos no terminales
        sb.append("\t<non-terminal-symbols>\n");
        ObservableList<String> noTermModel = this.getNoTerminalesModel(); // Asumimos que este método devuelve ObservableList<String>
        for (String nt : noTermModel) {
            sb.append("\t\t<non-terminal>\n");
            sb.append("\t\t\t<value>").append(nt).append("</value>\n");
            sb.append("\t\t</non-terminal>\n");
        }
        sb.append("\t</non-terminal-symbols>\n");

        // Sección de símbolos terminales
        sb.append("\t<terminal-symbols>\n");
        ObservableList<String> termModel = this.getTerminalesModel(); // Asumimos que este método devuelve ObservableList<String>
        for (String t : termModel) {
            sb.append("\t\t<terminal>\n");
            sb.append("\t\t\t<value>").append(t).append("</value>\n");
            sb.append("\t\t</terminal>\n");
        }
        sb.append("\t</terminal-symbols>\n");

        // Símbolo inicial
        sb.append("\t<init-symbol>").append(this.getSimbInicial()).append("</init-symbol>\n");

        // Sección de reglas (producciones)
        sb.append("\t<rule-set>\n");
        ObservableList<String> produccionesModel = this.getProduccionesModel(); // Asumimos que este método devuelve ObservableList<String>
        for (String prodStr : produccionesModel) {
            // Suponemos que cada producción está en el formato "Antecedente → simbolo1 simbolo2 ..."
            String[] partes = prodStr.split("→");
            if (partes.length < 2) continue; // Formato incorrecto; saltamos esta producción.

            String leftPart = partes[0].trim();
            String[] rightTokens = partes[1].trim().split(" ");
            StringBuilder rightPartBuilder = new StringBuilder();
            for (String rightToken : rightTokens) {
                rightPartBuilder.append("\n\t\t\t\t<symbol>\n");
                rightPartBuilder.append("\t\t\t\t\t<value>").append(rightToken.trim()).append("</value>\n");
                rightPartBuilder.append("\t\t\t\t</symbol>");
            }

            sb.append("\t\t<rule>\n");
            sb.append("\t\t\t<leftPart>\n\t\t\t\t<value>").append(leftPart).append("</value>\n\t\t\t</leftPart>\n");
            sb.append("\t\t\t<rightPart>").append(rightPartBuilder.toString()).append("\n\t\t\t</rightPart>\n");
            sb.append("\t\t</rule>\n");
        }
        sb.append("\t</rule-set>\n");
        sb.append("</grammar>\n");

        documentoXml = sb.toString();

        // Escribir el XML en el archivo usando try-with-resources para cerrar automáticamente
        try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
            out.write(documentoXml);
            return 1;
        } catch (IOException e) {
            // Aquí podrías registrar el error o construir un mensaje de error detallado
            // Por ejemplo:
            // String codigoError = "E-8";
            // String mensajeError = "Error de entrada-salida al guardar el fichero de gramática.";
            return -1;
        }
    }


    public Gramatica cargarGramatica(Window ownerWindow) {
        // Crear y configurar el FileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Gramaticas de SimAS (.xml)", "*.xml"));

        // Mostrar el diálogo y obtener el archivo seleccionado
        File file = fileChooser.showOpenDialog(ownerWindow);
        if (file == null) {
            // El usuario canceló la operación
            return null;
        }

        // Variables para almacenar información del XML
        String nombre = null;
        String descripcion = null;
        String simboloInicial = null;
        ObservableList<String> NT = FXCollections.observableArrayList();
        ObservableList<String> termModel = FXCollections.observableArrayList();
        ObservableList<String> prodModel = FXCollections.observableArrayList();

        try {
            // Preparar el analizador XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(file);
            doc.getDocumentElement().normalize();

            // Obtener la versión del documento
            Element root = doc.getDocumentElement();
            String version = root.getAttribute("version");

            // Extraer nombre y descripción
            NodeList nameList = root.getElementsByTagName("name");
            if (nameList.getLength() > 0) {
                nombre = nameList.item(0).getTextContent();
            }
            NodeList descList = root.getElementsByTagName("description");
            if (descList.getLength() > 0) {
                descripcion = descList.item(0).getTextContent();
            }

            // Crear la gramática
            Gramatica gramatica = new Gramatica(nombre, descripcion);

            // Extraer símbolos no terminales
            NodeList nodeNonTerminalSymbols = doc.getElementsByTagName("non-terminal");
            for (int i = 0; i < nodeNonTerminalSymbols.getLength(); i++) {
                Node nodo = nodeNonTerminalSymbols.item(i);
                if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                    Element elemento = (Element) nodo;
                    NodeList valueNode = elemento.getElementsByTagName("value");
                    if (valueNode.getLength() > 0) {
                        String ntValue = valueNode.item(0).getTextContent();
                        NT.add(ntValue);
                    }
                }
            }

            gramatica.setNoTerminalesModel(NT);

            // Extraer símbolos terminales
            NodeList nodeTerminalSymbols = doc.getElementsByTagName("terminal");
            for (int i = 0; i < nodeTerminalSymbols.getLength(); i++) {
                Node nodo = nodeTerminalSymbols.item(i);
                if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                    Element elemento = (Element) nodo;
                    NodeList valueNode = elemento.getElementsByTagName("value");
                    if (valueNode.getLength() > 0) {
                        String tValue = valueNode.item(0).getTextContent();
                        termModel.add(tValue);
                    }
                }
            }
            // Asumimos que gramatica tiene un método setTerminales que acepta ObservableList<String>
            gramatica.setTerminalesModel(termModel);

            // Actualizar el vocabulario (se puede llamar a setVocabulario si ya lo encapsula)
            gramatica.setVocabulario(NT, termModel);

            // Extraer símbolo inicial
            NodeList initSymbolList = root.getElementsByTagName("init-symbol");
            if (initSymbolList.getLength() > 0) {
                simboloInicial = initSymbolList.item(0).getTextContent();
                gramatica.setSimbInicial(simboloInicial);
            }

            // Extraer reglas (producciones)
            if (version.equals("1.0")) {
                NodeList nodeRules = doc.getElementsByTagName("rule");
                for (int i = 0; i < nodeRules.getLength(); i++) {
                    Node nodo = nodeRules.item(i);
                    if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                        Element elemento = (Element) nodo;
                        NodeList valueNode = elemento.getElementsByTagName("value");
                        if (valueNode.getLength() > 0) {
                            String ruleValue = valueNode.item(0).getTextContent();
                            prodModel.add(ruleValue);
                        }
                    }
                }
            } else if (version.equals("2.0")) {
                NodeList nodeRules = doc.getElementsByTagName("rule");
                for (int i = 0; i < nodeRules.getLength(); i++) {
                    Node nodo = nodeRules.item(i);
                    if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                        Element elemento = (Element) nodo;
                        // Procesar lado izquierdo
                        Node leftPart = elemento.getElementsByTagName("leftPart").item(0);
                        Element leftElement = (Element) leftPart;
                        NodeList leftValueNode = leftElement.getElementsByTagName("value");
                        String leftValue = (leftValueNode.getLength() > 0) ? leftValueNode.item(0).getTextContent() : "";

                        // Procesar lado derecho
                        Node rightPart = elemento.getElementsByTagName("rightPart").item(0);
                        Element rightElement = (Element) rightPart;
                        NodeList rightSymbols = rightElement.getElementsByTagName("symbol");
                        StringBuilder rightValue = new StringBuilder();
                        for (int k = 0; k < rightSymbols.getLength(); k++) {
                            Node nodoSimbolo = rightSymbols.item(k);
                            if (nodoSimbolo.getNodeType() == Node.ELEMENT_NODE) {
                                Element elementoSimbolo = (Element) nodoSimbolo;
                                NodeList rightValueNode = elementoSimbolo.getElementsByTagName("value");
                                if (rightValueNode.getLength() > 0) {
                                    rightValue.append(rightValueNode.item(0).getTextContent());
                                    if (k < rightSymbols.getLength() - 1) {
                                        rightValue.append(" ");
                                    }
                                }
                            }
                        }
                        // Formatear la producción con un separador "→"
                        String prodStr = leftValue + " → " + rightValue.toString();
                        prodModel.add(prodStr);
                    }
                }
            } else {
                // Versión por defecto similar a "1.0"
                NodeList nodeRules = doc.getElementsByTagName("rule");
                for (int i = 0; i < nodeRules.getLength(); i++) {
                    Node nodo = nodeRules.item(i);
                    if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                        Element elemento = (Element) nodo;
                        NodeList valueNode = elemento.getElementsByTagName("value");
                        if (valueNode.getLength() > 0) {
                            String ruleValue = valueNode.item(0).getTextContent();
                            prodModel.add(ruleValue);
                        }
                    }
                }
            }
            // Asumimos que gramatica tiene un método setProducciones que acepta ObservableList<String>
            gramatica.setProduccionesModel(prodModel);
            //gramatica.numerarProducciones();

            return gramatica;
        } catch (IOException e) {
            Logger.getLogger(Gramatica.class.getName()).log(Level.SEVERE, null, e);
        } catch (ParserConfigurationException e) {
            Logger.getLogger(Gramatica.class.getName()).log(Level.SEVERE, null, e);
        } catch (SAXException e) {
            Logger.getLogger(Gramatica.class.getName()).log(Level.SEVERE, null, e);
        }

        return null;
    }

    public ObservableList<String> validarGramatica() {
        // Usamos ObservableList para que, si se requiere, se pueda enlazar con la UI
        ObservableList<String> mensajesError = FXCollections.observableArrayList();
        // Variables locales para la validación
        ObservableList<Simbolo> conjSimbolos;
        this.setEstado(1);

        // Validar existencia de producciones
        if (this.producciones.isEmpty()) {
            this.setEstado(-1);
            mensajesError.add("No existen producciones.\nLa gramática no contiene ninguna producción. Debería contener al menos una para poder ser válida.");
        }

        // Validar existencia de símbolos terminales
        if (this.terminales.isEmpty()) {
            this.setEstado(-1);
            mensajesError.add("No existen símbolos terminales.\nLa gramática no contiene ningún símbolo terminal. Debería contener al menos uno para poder ser válida.");
        }

        // Validar existencia de símbolos no terminales
        if (this.noTerminales.isEmpty()) {
            this.setEstado(-1);
            mensajesError.add("No existen símbolos no terminales.\nLa gramática no contiene ningún símbolo no terminal. Debería contener al menos uno para poder ser válida.");
        }

        // Validar asignación del símbolo inicial
        if (this.getSimbInicial() == null) {
            this.setEstado(-1);
            mensajesError.add("Símbolo inicial no asignado.\nLa gramática no tiene asignado el símbolo inicial.");
        }

        // Validar que cada símbolo terminal aparezca en el consecuente de alguna producción
        for (Terminal t : this.terminales) {
            boolean encontrado = false;
            for (Produccion p : this.pr) {
                conjSimbolos = p.getConsec();
                for (Simbolo s : conjSimbolos) {
                    if (s.getValor().equals(t.getValor())) {
                        encontrado = true;
                        break;
                    }
                }
                if (encontrado) break;
            }
            if (!encontrado) {
                this.setEstado(-1);
                mensajesError.add("Símbolo terminal no usado.\n" +
                        "El símbolo terminal '" + t.getNombre() + "' no aparece en ningún consecuente de ninguna producción.");
            }
        }

        // Validar que cada símbolo no terminal aparezca en el consecuente de alguna producción
        for (NoTerminal nt : this.noTerminales) {
            boolean encontrado = false;
            for (Produccion p : this.pr) {
                conjSimbolos = p.getConsec();
                for (Simbolo s : conjSimbolos) {
                    // Si el símbolo no terminal es el símbolo inicial, se considera encontrado
                    if (nt.getValor().equals(this.getSimbInicial())) {
                        encontrado = true;
                        break;
                    } else if (s.getValor().equals(nt.getValor())) {
                        encontrado = true;
                        break;
                    }
                }
                if (encontrado) break;
            }
            if (!encontrado) {
                this.setEstado(-1);
                mensajesError.add("Simbolo no terminal no usado. " +
                        "El símbolo no terminal " + nt.getNombre() + " no aparece en ningún consecuente de ninguna producción.");
            }
        }

        // Validar que el antecedente de cada producción exista entre los símbolos no terminales
        for (Produccion p : this.pr) {
            Antecedente antecProd = p.getAntec();
            boolean encontrado = false;
            for (NoTerminal nt : this.noTerminales) {
                if (nt.getValor().equals(antecProd.getSimboloNT().getValor())) {
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                this.setEstado(-1);
                mensajesError.add("Simbolo no terminal no usado. " +
                        "El símbolo no terminal " + antecProd.getSimboloNT().getNombre() +
                        " no aparece en el antecedente de ninguna producción.");
            }
        }

        // Validar que cada símbolo del consecuente pertenezca al conjunto de símbolos declarado
        for (Produccion p : this.pr) {
            ObservableList<Simbolo> consec = p.getConsec();
            for (Simbolo s : consec) {
                boolean encontrado = false;
                // Si el símbolo es épsilon (representado por ε) se considera válido
                if (s.getValor().equals("ε")) {
                    encontrado = true;
                } else {
                    // Se busca en la lista de no terminales
                    for (NoTerminal nt : this.noTerminales) {
                        if (nt.getValor().equals(s.getValor())) {
                            encontrado = true;
                            break;
                        }
                    }
                    // Si aún no se encontró, se busca en la lista de terminales
                    if (!encontrado) {
                        for (Terminal t : this.terminales) {
                            if (t.getValor().equals(s.getValor())) {
                                encontrado = true;
                                break;
                            }
                        }
                    }
                }
                if (!encontrado) {
                    this.setEstado(-1);
                    mensajesError.add("Consecuente erróneo. " +
                            "El símbolo " + s.getNombre() + " del consecuente de la producción no pertenece al conjunto de símbolos declarado.");
                }
            }
        }
        return mensajesError;
    }


    public Boolean generarInforme(String fichero) throws DocumentException {
        // Método original para compatibilidad - usa español por defecto
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", new java.util.Locale("es"));
            return generarInforme(fichero, bundle);
        } catch (Exception e) {
            // Si no se puede cargar el bundle, usar valores por defecto
            return generarInforme(fichero, null);
        }
    }
    
    public Boolean generarInforme(String fichero, ResourceBundle bundle) throws DocumentException {
        // Solo genera el informe si la gramática está validada (estado==1)
        if (this.getEstado() == 1) {
            // Si no hay bundle, usar valores por defecto en español
            if (bundle == null) {
                bundle = new ResourceBundle() {
                    @Override
                    protected Object handleGetObject(String key) {
                        switch (key) {
                            case "informe.titulo": return "INFORME DE GRAMÁTICA";
                            case "informe.detalles": return "DETALLES DE LA GRAMÁTICA";
                            case "informe.descripcion": return "Descripción";
                            case "informe.simbolo.inicial": return "Símbolo Inicial";
                            case "informe.simbolos.no.terminales": return "Símbolos No Terminales";
                            case "informe.simbolos.terminales": return "Símbolos Terminales";
                            case "informe.producciones": return "Producciones";
                            case "informe.informacion.adicional": return "Información Adicional";
                            case "informe.estado.validacion": return "Estado de validación";
                            case "informe.numero.producciones": return "Número total de producciones";
                            case "informe.numero.no.terminales": return "Número de símbolos no terminales";
                            case "informe.numero.terminales": return "Número de símbolos terminales";
                            case "informe.fecha.generacion": return "Fecha de generación";
                            case "informe.documento.generado": return "Documento generado por SimAS v3.0 - Simulador de Análisis Sintáctico";
                            case "informe.pagina": return "Página";
                            default: return key;
                        }
                    }
                    
                    @Override
                    public Enumeration<String> getKeys() {
                        return Collections.enumeration(Arrays.asList(
                            "informe.titulo", "informe.detalles", "informe.descripcion", 
                            "informe.simbolo.inicial", "informe.simbolos.no.terminales", 
                            "informe.simbolos.terminales", "informe.producciones", 
                            "informe.informacion.adicional", "informe.estado.validacion",
                            "informe.numero.producciones", "informe.numero.no.terminales", 
                            "informe.numero.terminales", "informe.fecha.generacion", 
                            "informe.documento.generado", "informe.pagina"
                        ));
                    }
                };
            }
            try {
                // Configuración de la fuente y del documento PDF
                String fontPath = "fonts/arial.ttf";
                Document document = new Document(PageSize.A4, 50, 50, 80, 50);
                
                // Crear el PdfWriter con numeración de páginas
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fichero));
                
                // Configurar numeración de páginas
                final ResourceBundle finalBundle = bundle;
                writer.setPageEvent(new PdfPageEventHelper() {
                    @Override
                    public void onEndPage(PdfWriter writer, Document document) {
                        try {
                            BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                            Font font = new Font(bf, 10);
                            ColumnText.showTextAligned(writer.getDirectContent(), 
                                Paragraph.ALIGN_CENTER, 
                                new Phrase(String.format("%s %d", finalBundle.getString("informe.pagina"), writer.getPageNumber()), font), 
                                (document.right() - document.left()) / 2 + document.leftMargin(), 
                                document.bottom() - 10, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                
                // Cargar logo de la aplicación
                Image imagen = Image.getInstance(Objects.requireNonNull(getClass().getResource("/resources/logo2Antes.png")).toExternalForm());
                imagen.setAlignment(Image.ALIGN_CENTER);
                imagen.scalePercent(35);

                LineSeparator ls = new LineSeparator();
                BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                Font titulo = new Font(bf, 24, Font.BOLD);
                Font subtitulo = new Font(bf, 18, Font.BOLD);
                Font seccion = new Font(bf, 14, Font.BOLD);
                Font contenido = new Font(bf, 12);
                Font contenidoPequeno = new Font(bf, 10);
                BaseColor colorPrincipal = new BaseColor(33, 77, 72);
                BaseColor colorSecundario = new BaseColor(63, 171, 160);
                BaseColor colorAcento = new BaseColor(255, 140, 0);

                titulo.setColor(colorPrincipal);
                subtitulo.setColor(colorSecundario);
                seccion.setColor(colorPrincipal);

                ls.setLineWidth(2);
                ls.setLineColor(colorSecundario);

                document.open();
                
                // Página 1: Portada
                document.add(imagen);
                
                document.add(new Paragraph(" ", new Font(bf, 20))); // Espacio
                
                Paragraph parrafoTitulo = new Paragraph(bundle.getString("informe.titulo"), titulo);
                parrafoTitulo.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(parrafoTitulo);
                
                document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
                
                Paragraph parrafoSubtitulo = new Paragraph("SimAS - Simulador de Análisis Sintáctico", subtitulo);
                parrafoSubtitulo.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(parrafoSubtitulo);
                
                document.add(new Paragraph(" ", new Font(bf, 20))); // Espacio
                document.add(new Chunk(ls));
                document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
                
                // Solo el nombre de la gramática centrado con fuente más grande
                Font nombreGrande = new Font(bf, 20, Font.BOLD);
                nombreGrande.setColor(colorPrincipal);
                Paragraph parrafoNombre = new Paragraph(this.getNombre(), nombreGrande);
                parrafoNombre.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(parrafoNombre);
                
                // Nueva página para el contenido detallado
                document.newPage();
                
                // Título de la página de contenido
                Paragraph tituloContenido = new Paragraph(bundle.getString("informe.detalles"), subtitulo);
                tituloContenido.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(tituloContenido);
                document.add(new Chunk(ls));
                document.add(new Paragraph(" ", new Font(bf, 10))); // Espacio
                
                // Descripción de la gramática
                Paragraph parrafoDescripcion = new Paragraph(bundle.getString("informe.descripcion") + ":", seccion);
                document.add(parrafoDescripcion);
                Paragraph descripcion = new Paragraph("    " + this.getDescripcion(), contenido);
                descripcion.setIndentationLeft(20);
                document.add(descripcion);
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
                
                // Símbolo inicial
                Paragraph parrafoSimboloInicial = new Paragraph(bundle.getString("informe.simbolo.inicial") + ":", seccion);
                document.add(parrafoSimboloInicial);
                Paragraph simboloInicial = new Paragraph("    " + this.getSimbInicial(), contenido);
                simboloInicial.setIndentationLeft(20);
                document.add(simboloInicial);
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
                
                // Símbolos no terminales
                Paragraph parrafoNoTerminales = new Paragraph(bundle.getString("informe.simbolos.no.terminales") + ":", seccion);
                document.add(parrafoNoTerminales);
                ObservableList<String> noTermModel = this.getNoTerminalesModel();
                for (String nt : noTermModel) {
                    Paragraph noTerm = new Paragraph("    • " + nt, contenido);
                    noTerm.setIndentationLeft(20);
                    document.add(noTerm);
                }
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
                
                // Símbolos terminales
                Paragraph parrafoTerminales = new Paragraph(bundle.getString("informe.simbolos.terminales") + ":", seccion);
                document.add(parrafoTerminales);
                ObservableList<String> termModel = this.getTerminalesModel();
                for (String t : termModel) {
                    Paragraph term = new Paragraph("    • " + t, contenido);
                    term.setIndentationLeft(20);
                    document.add(term);
                }
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
                
                // Producciones
                Paragraph parrafoProducciones = new Paragraph(bundle.getString("informe.producciones") + ":", seccion);
                document.add(parrafoProducciones);
                
                ObservableList<String> prodModel = this.getProduccionesModel();
                int index = 1;
                for (String prod : prodModel) {
                    Paragraph produccion = new Paragraph("    " + index + ") " + prod, contenido);
                    produccion.setIndentationLeft(20);
                    document.add(produccion);
                    index++;
                }
                
                document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
                
                // Información adicional
                Paragraph parrafoInfo = new Paragraph(bundle.getString("informe.informacion.adicional") + ":", seccion);
                document.add(parrafoInfo);
                
                Paragraph infoValidacion = new Paragraph("    • " + bundle.getString("informe.estado.validacion") + ": VÁLIDA", contenido);
                infoValidacion.setIndentationLeft(20);
                document.add(infoValidacion);
                
                Paragraph infoProducciones = new Paragraph("    • " + bundle.getString("informe.numero.producciones") + ": " + prodModel.size(), contenido);
                infoProducciones.setIndentationLeft(20);
                document.add(infoProducciones);
                
                Paragraph infoNoTerminales = new Paragraph("    • " + bundle.getString("informe.numero.no.terminales") + ": " + noTermModel.size(), contenido);
                infoNoTerminales.setIndentationLeft(20);
                document.add(infoNoTerminales);
                
                Paragraph infoTerminales = new Paragraph("    • " + bundle.getString("informe.numero.terminales") + ": " + termModel.size(), contenido);
                infoTerminales.setIndentationLeft(20);
                document.add(infoTerminales);
                
                document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
                
                // Fecha de generación encima del pie de página
                Paragraph parrafoFecha = new Paragraph(bundle.getString("informe.fecha.generacion") + ": " + 
                    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), 
                    contenidoPequeno);
                parrafoFecha.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(parrafoFecha);
                
                document.add(new Paragraph(" ", new Font(bf, 5))); // Espacio pequeño
                
                // Pie de página con información de la aplicación
                Paragraph piePagina = new Paragraph(bundle.getString("informe.documento.generado"), contenidoPequeno);
                piePagina.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(piePagina);
                
                document.close();
                
            } catch (BadElementException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) { // Para capturar cualquier otra excepción
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Genera un informe PDF completo del simulador incluyendo gramática original, modificada y detalles de simulación.
     */
    public Boolean generarInformeSimulador(String fichero, Gramatica gramaticaOriginal, TablaPredictiva tablaPredictiva, 
                                         List<FuncionError> funcionesError, ResourceBundle bundle) throws DocumentException {
        try {
            // Configuración de la fuente y del documento PDF
            String fontPath = "fonts/arial.ttf";
            Document document = new Document(PageSize.A4, 50, 50, 80, 50);
            
            // Crear el PdfWriter con numeración de páginas
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fichero));
            
            // Configurar numeración de páginas
            final ResourceBundle finalBundle = bundle;
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        Font font = new Font(bf, 10);
                        ColumnText.showTextAligned(writer.getDirectContent(), 
                            Paragraph.ALIGN_CENTER, 
                            new Phrase(String.format("%s %d", finalBundle.getString("informe.pagina"), writer.getPageNumber()), font), 
                            (document.right() - document.left()) / 2 + document.leftMargin(), 
                            document.bottom() - 10, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            
            // Cargar logo de la aplicación
            Image imagen = Image.getInstance(Objects.requireNonNull(getClass().getResource("/resources/logo2Antes.png")).toExternalForm());
            imagen.setAlignment(Image.ALIGN_CENTER);
            imagen.scalePercent(35);

            LineSeparator ls = new LineSeparator();
            BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font titulo = new Font(bf, 24, Font.BOLD);
            Font subtitulo = new Font(bf, 18, Font.BOLD);
            Font seccion = new Font(bf, 14, Font.BOLD);
            Font contenido = new Font(bf, 12);
            Font contenidoPequeno = new Font(bf, 10);
            BaseColor colorPrincipal = new BaseColor(33, 77, 72);
            BaseColor colorSecundario = new BaseColor(63, 171, 160);
            BaseColor colorAcento = new BaseColor(255, 140, 0);

            titulo.setColor(colorPrincipal);
            subtitulo.setColor(colorSecundario);
            seccion.setColor(colorPrincipal);

            ls.setLineWidth(2);
            ls.setLineColor(colorSecundario);

            document.open();
            
            // Página 1: Portada
            document.add(imagen);
            
            document.add(new Paragraph(" ", new Font(bf, 20))); // Espacio
            
            Paragraph parrafoTitulo = new Paragraph(bundle.getString("informe.simulador.titulo"), titulo);
            parrafoTitulo.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(parrafoTitulo);
            
            document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
            
            Paragraph parrafoSubtitulo = new Paragraph("SimAS - Simulador de Análisis Sintáctico", subtitulo);
            parrafoSubtitulo.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(parrafoSubtitulo);
            
            document.add(new Paragraph(" ", new Font(bf, 20))); // Espacio
            document.add(new Chunk(ls));
            document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
            
            // Nombre de la gramática centrado
            Font nombreGrande = new Font(bf, 20, Font.BOLD);
            nombreGrande.setColor(colorPrincipal);
            Paragraph parrafoNombre = new Paragraph(this.getNombre(), nombreGrande);
            parrafoNombre.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(parrafoNombre);
            
            // Nueva página para el contenido detallado
            document.newPage();
            
            // Título de la página de contenido
            Paragraph tituloContenido = new Paragraph(bundle.getString("informe.simulador.titulo"), subtitulo);
            tituloContenido.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(tituloContenido);
            document.add(new Chunk(ls));
            document.add(new Paragraph(" ", new Font(bf, 10))); // Espacio
            
            // Descripción de la gramática
            Paragraph parrafoDescripcion = new Paragraph(bundle.getString("informe.descripcion") + ":", seccion);
            document.add(parrafoDescripcion);
            Paragraph descripcion = new Paragraph("    " + this.getDescripcion(), contenido);
            descripcion.setIndentationLeft(20);
            document.add(descripcion);
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Símbolo inicial
            Paragraph parrafoSimboloInicial = new Paragraph(bundle.getString("informe.simbolo.inicial") + ":", seccion);
            document.add(parrafoSimboloInicial);
            Paragraph simboloInicial = new Paragraph("    " + this.getSimbInicial(), contenido);
            simboloInicial.setIndentationLeft(20);
            document.add(simboloInicial);
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Transformaciones aplicadas
            Paragraph parrafoTransformaciones = new Paragraph(bundle.getString("informe.simulador.transformaciones") + ":", seccion);
            document.add(parrafoTransformaciones);
            
            // Verificar si se aplicaron transformaciones
            boolean esRecursiva = gramaticaOriginal.eliminarRecursividad();
            boolean necesitaFactorizacion = gramaticaOriginal.factorizar();
            
            if (esRecursiva || necesitaFactorizacion) {
                if (esRecursiva) {
                    Paragraph recursividad = new Paragraph("    • " + bundle.getString("informe.simulador.eliminacion.recursividad"), contenido);
                    recursividad.setIndentationLeft(20);
                    document.add(recursividad);
                }
                if (necesitaFactorizacion) {
                    Paragraph factorizacion = new Paragraph("    • " + bundle.getString("informe.simulador.factorizacion"), contenido);
                    factorizacion.setIndentationLeft(20);
                    document.add(factorizacion);
                }
            } else {
                Paragraph noTransformaciones = new Paragraph("    • " + bundle.getString("informe.simulador.no.transformaciones"), contenido);
                noTransformaciones.setIndentationLeft(20);
                document.add(noTransformaciones);
            }
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Gramática Original
            Paragraph parrafoGramaticaOriginal = new Paragraph(bundle.getString("informe.simulador.gramatica.original") + ":", seccion);
            document.add(parrafoGramaticaOriginal);
            
            // Símbolos originales
            Paragraph parrafoSimbolosOriginales = new Paragraph("    " + bundle.getString("informe.simulador.simbolos.originales") + ":", contenido);
            parrafoSimbolosOriginales.setIndentationLeft(20);
            document.add(parrafoSimbolosOriginales);
            
            // No terminales originales
            Paragraph parrafoNoTermOriginales = new Paragraph("        " + bundle.getString("informe.simbolos.no.terminales") + ":", contenido);
            parrafoNoTermOriginales.setIndentationLeft(20);
            document.add(parrafoNoTermOriginales);
            
            ObservableList<String> noTermOriginales = gramaticaOriginal.getNoTerminalesModel();
            for (String nt : noTermOriginales) {
                Paragraph noTerm = new Paragraph("            • " + nt, contenido);
                noTerm.setIndentationLeft(20);
                document.add(noTerm);
            }
            
            // Terminales originales
            Paragraph parrafoTermOriginales = new Paragraph("        " + bundle.getString("informe.simbolos.terminales") + ":", contenido);
            parrafoTermOriginales.setIndentationLeft(20);
            document.add(parrafoTermOriginales);
            
            ObservableList<String> termOriginales = gramaticaOriginal.getTerminalesModel();
            for (String t : termOriginales) {
                Paragraph term = new Paragraph("            • " + t, contenido);
                term.setIndentationLeft(20);
                document.add(term);
            }
            
            // Producciones originales
            Paragraph parrafoProduccionesOriginales = new Paragraph("    " + bundle.getString("informe.simulador.producciones.originales") + ":", contenido);
            parrafoProduccionesOriginales.setIndentationLeft(20);
            document.add(parrafoProduccionesOriginales);
            
            ObservableList<String> prodOriginales = gramaticaOriginal.getProduccionesModel();
            int index = 1;
            for (String prod : prodOriginales) {
                Paragraph produccion = new Paragraph("        " + index + ") " + prod, contenido);
                produccion.setIndentationLeft(20);
                document.add(produccion);
                index++;
            }
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Gramática Modificada
            Paragraph parrafoGramaticaModificada = new Paragraph(bundle.getString("informe.simulador.gramatica.modificada") + ":", seccion);
            document.add(parrafoGramaticaModificada);
            
            // Símbolos modificados
            Paragraph parrafoSimbolosModificados = new Paragraph("    " + bundle.getString("informe.simulador.simbolos.modificados") + ":", contenido);
            parrafoSimbolosModificados.setIndentationLeft(20);
            document.add(parrafoSimbolosModificados);
            
            // No terminales modificados
            Paragraph parrafoNoTermModificados = new Paragraph("        " + bundle.getString("informe.simbolos.no.terminales") + ":", contenido);
            parrafoNoTermModificados.setIndentationLeft(20);
            document.add(parrafoNoTermModificados);
            
            ObservableList<String> noTermModificados = this.getNoTerminalesModel();
            for (String nt : noTermModificados) {
                Paragraph noTerm = new Paragraph("            • " + nt, contenido);
                noTerm.setIndentationLeft(20);
                document.add(noTerm);
            }
            
            // Terminales modificados
            Paragraph parrafoTermModificados = new Paragraph("        " + bundle.getString("informe.simbolos.terminales") + ":", contenido);
            parrafoTermModificados.setIndentationLeft(20);
            document.add(parrafoTermModificados);
            
            ObservableList<String> termModificados = this.getTerminalesModel();
            for (String t : termModificados) {
                Paragraph term = new Paragraph("            • " + t, contenido);
                term.setIndentationLeft(20);
                document.add(term);
            }
            
            // Producciones modificadas
            Paragraph parrafoProduccionesModificadas = new Paragraph("    " + bundle.getString("informe.simulador.producciones.modificadas") + ":", contenido);
            parrafoProduccionesModificadas.setIndentationLeft(20);
            document.add(parrafoProduccionesModificadas);
            
            ObservableList<String> prodModificadas = this.getProduccionesModel();
            index = 1;
            for (String prod : prodModificadas) {
                Paragraph produccion = new Paragraph("        " + index + ") " + prod, contenido);
                produccion.setIndentationLeft(20);
                document.add(produccion);
                index++;
            }
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Tabla Predictiva
            if (tablaPredictiva != null) {
                Paragraph parrafoTablaPredictiva = new Paragraph(bundle.getString("informe.simulador.tabla.predictiva") + ":", seccion);
                document.add(parrafoTablaPredictiva);
                
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio entre título y tabla
                
                // Agregar tabla predictiva directamente
                agregarTablaPredictivaAlPDF(document, tablaPredictiva, bundle, bf, contenido);
                
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            }
            
            // Funciones de Error
            if (funcionesError != null && !funcionesError.isEmpty()) {
                Paragraph parrafoFuncionesError = new Paragraph(bundle.getString("informe.simulador.funciones.error") + ":", seccion);
                document.add(parrafoFuncionesError);
                
                for (int i = 0; i < funcionesError.size(); i++) {
                    FuncionError fe = funcionesError.get(i);
                    String descripcionFuncion = getDescripcionFuncionError(fe, bundle);
                    Paragraph funcion = new Paragraph("    " + i + ". " + descripcionFuncion, contenido);
                    funcion.setIndentationLeft(20);
                    document.add(funcion);
                }
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            }
            
            // Información adicional
            Paragraph parrafoInfo = new Paragraph(bundle.getString("informe.informacion.adicional") + ":", seccion);
            document.add(parrafoInfo);
            
            Paragraph infoProduccionesOriginales = new Paragraph("    • " + bundle.getString("informe.simulador.producciones.originales") + ": " + prodOriginales.size(), contenido);
            infoProduccionesOriginales.setIndentationLeft(20);
            document.add(infoProduccionesOriginales);
            
            Paragraph infoProduccionesModificadas = new Paragraph("    • " + bundle.getString("informe.simulador.producciones.modificadas") + ": " + prodModificadas.size(), contenido);
            infoProduccionesModificadas.setIndentationLeft(20);
            document.add(infoProduccionesModificadas);
            
            Paragraph infoNoTerminalesOriginales = new Paragraph("    • " + bundle.getString("informe.simulador.simbolos.originales") + " (No Terminales): " + noTermOriginales.size(), contenido);
            infoNoTerminalesOriginales.setIndentationLeft(20);
            document.add(infoNoTerminalesOriginales);
            
            Paragraph infoTerminalesOriginales = new Paragraph("    • " + bundle.getString("informe.simulador.simbolos.originales") + " (Terminales): " + termOriginales.size(), contenido);
            infoTerminalesOriginales.setIndentationLeft(20);
            document.add(infoTerminalesOriginales);
            
            Paragraph infoNoTerminalesModificados = new Paragraph("    • " + bundle.getString("informe.simulador.simbolos.modificados") + " (No Terminales): " + noTermModificados.size(), contenido);
            infoNoTerminalesModificados.setIndentationLeft(20);
            document.add(infoNoTerminalesModificados);
            
            Paragraph infoTerminalesModificados = new Paragraph("    • " + bundle.getString("informe.simulador.simbolos.modificados") + " (Terminales): " + termModificados.size(), contenido);
            infoTerminalesModificados.setIndentationLeft(20);
            document.add(infoTerminalesModificados);
            
            document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
            
            // Fecha de generación encima del pie de página
            Paragraph parrafoFecha = new Paragraph(bundle.getString("informe.fecha.generacion") + ": " + 
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), 
                contenidoPequeno);
            parrafoFecha.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(parrafoFecha);
            
            document.add(new Paragraph(" ", new Font(bf, 5))); // Espacio pequeño
            
            // Pie de página con información de la aplicación
            Paragraph piePagina = new Paragraph(bundle.getString("informe.documento.generado"), contenidoPequeno);
            piePagina.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(piePagina);
            
            document.close();
            
        } catch (BadElementException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    /**
     * Genera un informe PDF completo del simulador incluyendo gramática original, modificada y detalles de simulación.
     */
    public Boolean generarInformeSimulacionFinal(String fichero, Gramatica gramaticaOriginal, TablaPredictiva tablaPredictiva,
                                         List<FuncionError> funcionesError, ResourceBundle bundle, String cadenaEntrada,
                                         String estadoSimulacion, List<HistorialPaso> historialPasos) throws DocumentException {
        try {
            // Configuración de la fuente y del documento PDF
            String fontPath = "fonts/arial.ttf";
            Document document = new Document(PageSize.A4, 50, 50, 80, 50);
            
            // Crear el PdfWriter con numeración de páginas
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fichero));
            
            // Configurar numeración de páginas
            final ResourceBundle finalBundle = bundle;
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                        Font font = new Font(bf, 10);
                        ColumnText.showTextAligned(writer.getDirectContent(), 
                            Paragraph.ALIGN_CENTER, 
                            new Phrase(String.format("%s %d", finalBundle.getString("informe.pagina"), writer.getPageNumber()), font), 
                            (document.right() - document.left()) / 2 + document.leftMargin(), 
                            document.bottom() - 10, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            
            // Cargar logo de la aplicación
            Image imagen = Image.getInstance(Objects.requireNonNull(getClass().getResource("/resources/logo2Antes.png")).toExternalForm());
            imagen.setAlignment(Image.ALIGN_CENTER);
            imagen.scalePercent(35);

            LineSeparator ls = new LineSeparator();
            BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font titulo = new Font(bf, 24, Font.BOLD);
            Font subtitulo = new Font(bf, 18, Font.BOLD);
            Font seccion = new Font(bf, 14, Font.BOLD);
            Font contenido = new Font(bf, 12);
            Font contenidoPequeno = new Font(bf, 10);
            BaseColor colorPrincipal = new BaseColor(33, 77, 72);
            BaseColor colorSecundario = new BaseColor(63, 171, 160);
            BaseColor colorAcento = new BaseColor(255, 140, 0);

            titulo.setColor(colorPrincipal);
            subtitulo.setColor(colorSecundario);
            seccion.setColor(colorPrincipal);

            ls.setLineWidth(2);
            ls.setLineColor(colorSecundario);

            document.open();
            
            // Página 1: Portada
            document.add(imagen);
            
            document.add(new Paragraph(" ", new Font(bf, 20))); // Espacio
            
            Paragraph parrafoTitulo = new Paragraph(bundle.getString("informe.simulador.titulo"), titulo);
            parrafoTitulo.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(parrafoTitulo);
            
            document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
            
            Paragraph parrafoSubtitulo = new Paragraph("SimAS - Simulador de Análisis Sintáctico", subtitulo);
            parrafoSubtitulo.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(parrafoSubtitulo);
            
            document.add(new Paragraph(" ", new Font(bf, 20))); // Espacio
            document.add(new Chunk(ls));
            document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio
            
            // Nombre de la gramática centrado
            Font nombreGrande = new Font(bf, 20, Font.BOLD);
            nombreGrande.setColor(colorPrincipal);
            Paragraph parrafoNombre = new Paragraph(this.getNombre(), nombreGrande);
            parrafoNombre.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(parrafoNombre);
            
            // Nueva página para el contenido detallado
            document.newPage();
            
            // Título de la página de contenido
            Paragraph tituloContenido = new Paragraph(bundle.getString("informe.simulador.titulo"), subtitulo);
            tituloContenido.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(tituloContenido);
            document.add(new Chunk(ls));
            document.add(new Paragraph(" ", new Font(bf, 10))); // Espacio
            
            // Descripción de la gramática
            Paragraph parrafoDescripcion = new Paragraph(bundle.getString("informe.descripcion") + ":", seccion);
            document.add(parrafoDescripcion);
            Paragraph descripcion = new Paragraph("    " + this.getDescripcion(), contenido);
            descripcion.setIndentationLeft(20);
            document.add(descripcion);
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Símbolo inicial
            Paragraph parrafoSimboloInicial = new Paragraph(bundle.getString("informe.simbolo.inicial") + ":", seccion);
            document.add(parrafoSimboloInicial);
            Paragraph simboloInicial = new Paragraph("    " + this.getSimbInicial(), contenido);
            simboloInicial.setIndentationLeft(20);
            document.add(simboloInicial);
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Transformaciones aplicadas
            Paragraph parrafoTransformaciones = new Paragraph(bundle.getString("informe.simulador.transformaciones") + ":", seccion);
            document.add(parrafoTransformaciones);
            
            // Verificar si se aplicaron transformaciones
            boolean esRecursiva = gramaticaOriginal.eliminarRecursividad();
            boolean necesitaFactorizacion = gramaticaOriginal.factorizar();
            
            if (esRecursiva || necesitaFactorizacion) {
                if (esRecursiva) {
                    Paragraph recursividad = new Paragraph("    • " + bundle.getString("informe.simulador.eliminacion.recursividad"), contenido);
                    recursividad.setIndentationLeft(20);
                    document.add(recursividad);
                }
                if (necesitaFactorizacion) {
                    Paragraph factorizacion = new Paragraph("    • " + bundle.getString("informe.simulador.factorizacion"), contenido);
                    factorizacion.setIndentationLeft(20);
                    document.add(factorizacion);
                }
            } else {
                Paragraph noTransformaciones = new Paragraph("    • " + bundle.getString("informe.simulador.no.transformaciones"), contenido);
                noTransformaciones.setIndentationLeft(20);
                document.add(noTransformaciones);
            }
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Gramática Original
            Paragraph parrafoGramaticaOriginal = new Paragraph(bundle.getString("informe.simulador.gramatica.original") + ":", seccion);
            document.add(parrafoGramaticaOriginal);
            
            // Símbolos originales
            Paragraph parrafoSimbolosOriginales = new Paragraph("    " + bundle.getString("informe.simulador.simbolos.originales") + ":", contenido);
            parrafoSimbolosOriginales.setIndentationLeft(20);
            document.add(parrafoSimbolosOriginales);
            
            // No terminales originales
            Paragraph parrafoNoTermOriginales = new Paragraph("        " + bundle.getString("informe.simbolos.no.terminales") + ":", contenido);
            parrafoNoTermOriginales.setIndentationLeft(20);
            document.add(parrafoNoTermOriginales);
            
            ObservableList<String> noTermOriginales = gramaticaOriginal.getNoTerminalesModel();
            for (String nt : noTermOriginales) {
                Paragraph noTerm = new Paragraph("            • " + nt, contenido);
                noTerm.setIndentationLeft(20);
                document.add(noTerm);
            }
            
            // Terminales originales
            Paragraph parrafoTermOriginales = new Paragraph("        " + bundle.getString("informe.simbolos.terminales") + ":", contenido);
            parrafoTermOriginales.setIndentationLeft(20);
            document.add(parrafoTermOriginales);
            
            ObservableList<String> termOriginales = gramaticaOriginal.getTerminalesModel();
            for (String t : termOriginales) {
                Paragraph term = new Paragraph("            • " + t, contenido);
                term.setIndentationLeft(20);
                document.add(term);
            }
            
            // Producciones originales
            Paragraph parrafoProduccionesOriginales = new Paragraph("    " + bundle.getString("informe.simulador.producciones.originales") + ":", contenido);
            parrafoProduccionesOriginales.setIndentationLeft(20);
            document.add(parrafoProduccionesOriginales);
            
            ObservableList<String> prodOriginales = gramaticaOriginal.getProduccionesModel();
            int index = 1;
            for (String prod : prodOriginales) {
                Paragraph produccion = new Paragraph("        " + index + ") " + prod, contenido);
                produccion.setIndentationLeft(20);
                document.add(produccion);
                index++;
            }
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Gramática Modificada
            Paragraph parrafoGramaticaModificada = new Paragraph(bundle.getString("informe.simulador.gramatica.modificada") + ":", seccion);
            document.add(parrafoGramaticaModificada);
            
            // Símbolos modificados
            Paragraph parrafoSimbolosModificados = new Paragraph("    " + bundle.getString("informe.simulador.simbolos.modificados") + ":", contenido);
            parrafoSimbolosModificados.setIndentationLeft(20);
            document.add(parrafoSimbolosModificados);
            
            // No terminales modificados
            Paragraph parrafoNoTermModificados = new Paragraph("        " + bundle.getString("informe.simbolos.no.terminales") + ":", contenido);
            parrafoNoTermModificados.setIndentationLeft(20);
            document.add(parrafoNoTermModificados);
            
            ObservableList<String> noTermModificados = this.getNoTerminalesModel();
            for (String nt : noTermModificados) {
                Paragraph noTerm = new Paragraph("            • " + nt, contenido);
                noTerm.setIndentationLeft(20);
                document.add(noTerm);
            }
            
            // Terminales modificados
            Paragraph parrafoTermModificados = new Paragraph("        " + bundle.getString("informe.simbolos.terminales") + ":", contenido);
            parrafoTermModificados.setIndentationLeft(20);
            document.add(parrafoTermModificados);
            
            ObservableList<String> termModificados = this.getTerminalesModel();
            for (String t : termModificados) {
                Paragraph term = new Paragraph("            • " + t, contenido);
                term.setIndentationLeft(20);
                document.add(term);
            }
            
            // Producciones modificadas
            Paragraph parrafoProduccionesModificadas = new Paragraph("    " + bundle.getString("informe.simulador.producciones.modificadas") + ":", contenido);
            parrafoProduccionesModificadas.setIndentationLeft(20);
            document.add(parrafoProduccionesModificadas);
            
            ObservableList<String> prodModificadas = this.getProduccionesModel();
            index = 1;
            for (String prod : prodModificadas) {
                Paragraph produccion = new Paragraph("        " + index + ") " + prod, contenido);
                produccion.setIndentationLeft(20);
                document.add(produccion);
                index++;
            }
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Tabla Predictiva
            if (tablaPredictiva != null) {
                Paragraph parrafoTablaPredictiva = new Paragraph(bundle.getString("informe.simulador.tabla.predictiva") + ":", seccion);
                document.add(parrafoTablaPredictiva);
                
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio entre título y tabla
                
                // Agregar tabla predictiva directamente
                agregarTablaPredictivaAlPDF(document, tablaPredictiva, bundle, bf, contenido);
                
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            }
            
            // Funciones de Error
            if (funcionesError != null && !funcionesError.isEmpty()) {
                Paragraph parrafoFuncionesError = new Paragraph(bundle.getString("informe.simulador.funciones.error") + ":", seccion);
                document.add(parrafoFuncionesError);
                
                for (int i = 0; i < funcionesError.size(); i++) {
                    FuncionError fe = funcionesError.get(i);
                    String descripcionFuncion = getDescripcionFuncionError(fe, bundle);
                    Paragraph funcion = new Paragraph("    " + i + ". " + descripcionFuncion, contenido);
                    funcion.setIndentationLeft(20);
                    document.add(funcion);
                }
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            }
            
            // Información adicional
            Paragraph parrafoInfo = new Paragraph(bundle.getString("informe.informacion.adicional") + ":", seccion);
            document.add(parrafoInfo);
            
            Paragraph infoProduccionesOriginales = new Paragraph("    • " + bundle.getString("informe.simulador.producciones.originales") + ": " + prodOriginales.size(), contenido);
            infoProduccionesOriginales.setIndentationLeft(20);
            document.add(infoProduccionesOriginales);
            
            Paragraph infoProduccionesModificadas = new Paragraph("    • " + bundle.getString("informe.simulador.producciones.modificadas") + ": " + prodModificadas.size(), contenido);
            infoProduccionesModificadas.setIndentationLeft(20);
            document.add(infoProduccionesModificadas);
            
            Paragraph infoNoTerminalesOriginales = new Paragraph("    • " + bundle.getString("informe.simulador.simbolos.originales") + " (No Terminales): " + noTermOriginales.size(), contenido);
            infoNoTerminalesOriginales.setIndentationLeft(20);
            document.add(infoNoTerminalesOriginales);
            
            Paragraph infoTerminalesOriginales = new Paragraph("    • " + bundle.getString("informe.simulador.simbolos.originales") + " (Terminales): " + termOriginales.size(), contenido);
            infoTerminalesOriginales.setIndentationLeft(20);
            document.add(infoTerminalesOriginales);
            
            Paragraph infoNoTerminalesModificados = new Paragraph("    • " + bundle.getString("informe.simulador.simbolos.modificados") + " (No Terminales): " + noTermModificados.size(), contenido);
            infoNoTerminalesModificados.setIndentationLeft(20);
            document.add(infoNoTerminalesModificados);
            
            Paragraph infoTerminalesModificados = new Paragraph("    • " + bundle.getString("informe.simulador.simbolos.modificados") + " (Terminales): " + termModificados.size(), contenido);
            infoTerminalesModificados.setIndentationLeft(20);
            document.add(infoTerminalesModificados);

            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio

            // Información de la simulación
            Paragraph parrafoSimulacion = new Paragraph("Información de la Simulación:", seccion);
            document.add(parrafoSimulacion);
            
            // Cadena de entrada
            Paragraph parrafoCadenaEntrada = new Paragraph("    Cadena de Entrada:", contenido);
            parrafoCadenaEntrada.setIndentationLeft(20);
            document.add(parrafoCadenaEntrada);
            Paragraph parrafoCadena = new Paragraph("        " + (cadenaEntrada != null ? cadenaEntrada : "No especificada"), contenido);
            parrafoCadena.setIndentationLeft(20);
            document.add(parrafoCadena);
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            
            // Estado de la simulación
            Paragraph parrafoEstadoSimulacion = new Paragraph("    Estado de la Simulación:", contenido);
            parrafoEstadoSimulacion.setIndentationLeft(20);
            document.add(parrafoEstadoSimulacion);
            
            // Determinar el color del estado
            BaseColor colorEstado;
            if (estadoSimulacion != null && estadoSimulacion.equals("ACEPTADA")) {
                colorEstado = new BaseColor(0, 128, 0); // Verde para aceptada
            } else if (estadoSimulacion != null && estadoSimulacion.equals("RECHAZADA")) {
                colorEstado = new BaseColor(255, 0, 0); // Rojo para rechazada
            } else {
                colorEstado = new BaseColor(128, 128, 128); // Gris para no especificado
            }
            
            Font estadoFont = new Font(bf, 12, Font.BOLD);
            estadoFont.setColor(colorEstado);
            Paragraph estadoSim = new Paragraph("        " + (estadoSimulacion != null ? estadoSimulacion : "No especificado"), estadoFont);
            estadoSim.setIndentationLeft(20);
            document.add(estadoSim);
            document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio

            // Historial de pasos
            if (historialPasos != null && !historialPasos.isEmpty()) {
                Paragraph parrafoHistorial = new Paragraph("    Historial de Pasos:", contenido);
                parrafoHistorial.setIndentationLeft(20);
                document.add(parrafoHistorial);
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio pequeño

                // Crear tabla para el historial
                PdfPTable tablaHistorial = new PdfPTable(4);
                tablaHistorial.setWidthPercentage(100);
                tablaHistorial.setWidths(new float[]{1, 2, 2, 3});

                // Encabezados
                Font headerFont = new Font(bf, 10, Font.BOLD);
                headerFont.setColor(colorSecundario);
                PdfPCell cellPaso = new PdfPCell(new Phrase("Paso", headerFont));
                cellPaso.setBackgroundColor(colorPrincipal);
                cellPaso.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                tablaHistorial.addCell(cellPaso);

                PdfPCell cellPila = new PdfPCell(new Phrase("Pila", headerFont));
                cellPila.setBackgroundColor(colorPrincipal);
                cellPila.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                tablaHistorial.addCell(cellPila);

                PdfPCell cellEntrada = new PdfPCell(new Phrase("Entrada", headerFont));
                cellEntrada.setBackgroundColor(colorPrincipal);
                cellEntrada.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                tablaHistorial.addCell(cellEntrada);

                PdfPCell cellAccion = new PdfPCell(new Phrase("Acción", headerFont));
                cellAccion.setBackgroundColor(colorPrincipal);
                cellAccion.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                tablaHistorial.addCell(cellAccion);

                // Datos
                Font dataFont = new Font(bf, 9);
                for (HistorialPaso paso : historialPasos) {
                    PdfPCell cellPasoData = new PdfPCell(new Phrase(paso.getPaso(), dataFont));
                    cellPasoData.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                    tablaHistorial.addCell(cellPasoData);

                    PdfPCell cellPilaData = new PdfPCell(new Phrase(paso.getPila(), dataFont));
                    cellPilaData.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                    tablaHistorial.addCell(cellPilaData);

                    PdfPCell cellEntradaData = new PdfPCell(new Phrase(paso.getEntrada(), dataFont));
                    cellEntradaData.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                    tablaHistorial.addCell(cellEntradaData);

                    PdfPCell cellAccionData = new PdfPCell(new Phrase(paso.getAccion(), dataFont));
                    cellAccionData.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
                    tablaHistorial.addCell(cellAccionData);
                }

                document.add(tablaHistorial);
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            }

            // Derivación
            if (historialPasos != null && !historialPasos.isEmpty()) {
                Paragraph parrafoDerivacion = new Paragraph("    Derivación:", contenido);
                parrafoDerivacion.setIndentationLeft(20);
                document.add(parrafoDerivacion);
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio pequeño

                // Generar derivación basada en el historial
                for (int i = 0; i < historialPasos.size(); i++) {
                    HistorialPaso paso = historialPasos.get(i);
                    String derivacionLine = "        Paso " + (i + 1) + ": " + paso.getAccion();
                    Paragraph derivacionPaso = new Paragraph(derivacionLine, contenido);
                    derivacionPaso.setIndentationLeft(20);
                    document.add(derivacionPaso);
                }
                document.add(new Paragraph(" ", new Font(bf, 8))); // Espacio
            }

            // Nota sobre el árbol sintáctico
            Paragraph parrafoArbol = new Paragraph("    Árbol Sintáctico:", contenido);
            parrafoArbol.setIndentationLeft(20);
            document.add(parrafoArbol);
            Paragraph notaArbol = new Paragraph("        El árbol sintáctico completo se encuentra disponible en la interfaz de usuario del simulador.", contenidoPequeno);
            notaArbol.setIndentationLeft(20);
            document.add(notaArbol);

            document.add(new Paragraph(" ", new Font(bf, 15))); // Espacio

            // Fecha de generación encima del pie de página
            Paragraph parrafoFecha = new Paragraph(bundle.getString("informe.fecha.generacion") + ": " + 
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), 
                contenidoPequeno);
            parrafoFecha.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(parrafoFecha);
            document.add(new Paragraph(" ", new Font(bf, 5))); // Espacio pequeño
            
            // Pie de página con información de la aplicación
            Paragraph piePagina = new Paragraph(bundle.getString("informe.documento.generado"), contenidoPequeno);
            piePagina.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(piePagina);
            
            document.close();
            
        } catch (BadElementException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    public boolean isNoTerminal(String nombre) {
        for (NoTerminal nt : this.getNoTerminales()) {
            if (nt.getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTerminal(String nombre) {
        for (Terminal t : this.getTerminales()) {
            if (t.getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    public void copiarDesde(Gramatica otra) {
        this.nombre.set(otra.getNombre());
        this.descripcion.set(otra.getDescripcion());
        this.simbInicial.set(otra.getSimbInicial());
        this.estado.set(otra.getEstado());

        this.noTerm.setAll(otra.getNoTerminalesModel());
        this.term.setAll(otra.getTerminalesModel());
        this.producciones.setAll(otra.getProduccionesModel());

        this.noTerminales.setAll(otra.getNoTerminales());
        this.terminales.setAll(otra.getTerminales());
        this.pr.setAll(otra.getProducciones());
    }

    public void modificarSimboloProduccion(String simboloAntiguo, String nuevoSimbolo) {
        for (Produccion produccion : pr) {
            produccion.modificarSimbolo(simboloAntiguo, nuevoSimbolo);
        }
    }

    //SIMULACION

    public void generarTPredictiva() {
        // Si ya existe una tabla predictiva, limpiarla
        if (this.tpredictiva != null) {
            this.tpredictiva = null;
        }
        // Crear una nueva tabla predictiva
        this.tpredictiva = new TablaPredictiva();
    }

    public TablaPredictiva getTPredictiva() {
        return this.tpredictiva;
    }

    public void setTPredictiva(TablaPredictiva tabla) {
        this.tpredictiva = tabla;
    }

    /**
     * Verifica si la gramática tiene recursividad por la izquierda y la elimina.
     *
     * @return true si la gramática tenía recursividad y fue modificada, false si ya estaba correcta.
     */
    public boolean eliminarRecursividad() {

        boolean esRecursiva = false;
        ObservableList<String> produccionesOriginales = getProduccionesModel();
        ObservableList<String> produccionesModificadas = FXCollections.observableArrayList();
        ObservableList<String> noTerminalesModificados = getNoTerminalesModel();

        for (String produccion : produccionesOriginales) {
            String[] partes = produccion.split(" ");
            String antecedente = partes[0];

            // Verificar recursividad por la izquierda
            if (partes.length > 2 && antecedente.equals(partes[2])) {
                esRecursiva = true;
                String nuevoNoTerminal = antecedente + "'";

                if (!noTerminalesModificados.contains(nuevoNoTerminal)) {
                    noTerminalesModificados.add(nuevoNoTerminal);
                }

                // Nueva producción sin recursividad
                StringBuilder nuevaProduccion = new StringBuilder(antecedente + " →");
                for (int i = 3; i < partes.length; i++) {
                    nuevaProduccion.append(" ").append(partes[i]);
                }
                nuevaProduccion.append(" ").append(nuevoNoTerminal);
                produccionesModificadas.add(nuevaProduccion.toString());

                // Agregar la nueva producción con ε
                produccionesModificadas.add(nuevoNoTerminal + " → ε");
            } else {
                produccionesModificadas.add(produccion);
            }
        }

        // **🔴 Se actualiza la copia de las producciones en lugar de las originales**
        if (esRecursiva) {
            this.setNoTerminalesModel(noTerminalesModificados);
            this.setProduccionesModel(produccionesModificadas);
        }

        return esRecursiva;
    }


    /**
     * Verifica si la gramática requiere factorización y la realiza si es necesario.
     *
     * @return true si la gramática fue factorizada, false si ya estaba correcta.
     */
    public boolean factorizar() {

        boolean necesitaFactorizacion = false;
        ObservableList<String> produccionesOriginales = getProduccionesModel();
        ObservableList<String> produccionesModificadas = FXCollections.observableArrayList();
        ObservableList<String> noTerminales = getNoTerminalesModel();

        Map<String, List<String>> gruposProducciones = new HashMap<>();

        // Agrupar producciones por antecedente
        for (String produccion : produccionesOriginales) {
            String[] partes = produccion.split(" ");
            String antecedente = partes[0];
            String clave = antecedente + " → " + partes[2]; // Agrupar por el primer símbolo después de la flecha

            gruposProducciones.putIfAbsent(clave, new ArrayList<>());
            gruposProducciones.get(clave).add(produccion);
        }

        // Revisar si hay producción con prefijo común
        for (Map.Entry<String, List<String>> grupo : gruposProducciones.entrySet()) {
            List<String> listaProducciones = grupo.getValue();
            String antecedente = listaProducciones.get(0).split(" ")[0];

            if (listaProducciones.size() > 1) { // Hay factor común
                necesitaFactorizacion = true;
                String nuevoNoTerminal = antecedente + "'";

                if (!noTerminales.contains(nuevoNoTerminal)) {
                    noTerminales.add(nuevoNoTerminal);
                }

                // Nueva producción con factor común extraído
                produccionesModificadas.add(antecedente + " → " + listaProducciones.get(0).split(" ")[2] + " " + nuevoNoTerminal);

                for (String produccion : listaProducciones) {
                    String[] partes = produccion.split(" ");
                    StringBuilder nuevaProduccion = new StringBuilder(nuevoNoTerminal + " →");

                    if (partes.length > 3) {
                        for (int i = 3; i < partes.length; i++) {
                            nuevaProduccion.append(" ").append(partes[i]);
                        }
                    } else {
                        nuevaProduccion.append(" ε");
                    }

                    produccionesModificadas.add(nuevaProduccion.toString());
                }
            } else {
                produccionesModificadas.addAll(listaProducciones);
            }
        }

        // **🔴 Se actualiza la copia de las producciones en lugar de las originales**
        if (necesitaFactorizacion) {
            this.setNoTerminalesModel(noTerminales);
            this.setProduccionesModel(produccionesModificadas);
        }

        return necesitaFactorizacion;
    }


    public void generarConjPrim() {
        Map<String, Set<String>> primeros = new HashMap<>();
        Map<String, Set<String>> dependencias = new HashMap<>();

        // Inicializar conjuntos primeros para cada no terminal
        for (NoTerminal nt : this.noTerminales) {
            primeros.put(nt.getNombre(), new HashSet<>());
        }

        // Primer recorrido: agregar terminales y épsilon directamente
        for (Produccion pr : this.getProducciones()) {
            String antecedente = pr.getAntec().getSimboloNT().getNombre();
            Simbolo primero = pr.getConsec().get(0);

            if (primero.getNombre().equals("\u03b5")) {
                primeros.get(antecedente).add("\u03b5");
            } else if (isTerminal(primero.getNombre())) {
                primeros.get(antecedente).add(primero.getNombre());
            } else {
                dependencias.computeIfAbsent(antecedente, k -> new HashSet<>()).add(primero.getNombre());
            }
        }

        // Resolver dependencias
        boolean cambios;
        do {
            cambios = false;
            for (Map.Entry<String, Set<String>> entry : dependencias.entrySet()) {
                String antecedente = entry.getKey();
                Set<String> dependientes = entry.getValue();
                for (String dependiente : dependientes) {
                    int sizeAntes = primeros.get(antecedente).size();
                    primeros.get(antecedente).addAll(primeros.get(dependiente));
                    if (primeros.get(antecedente).size() > sizeAntes) {
                        cambios = true;
                    }
                }
            }
        } while (cambios);

        // Actualizar los conjuntos primeros en los no terminales
        for (NoTerminal nt : this.noTerminales) {
            Set<String> conjuntoPrimero = primeros.get(nt.getNombre());
            ObservableList<Terminal> listaPrimero = FXCollections.observableArrayList();
            for (String simbolo : conjuntoPrimero) {
                listaPrimero.add(new Terminal(simbolo, simbolo));
            }
            nt.setPrimeros(listaPrimero);
        }
    }

    public void generarConjSig() {
        Map<String, Set<String>> siguientes = new HashMap<>();

        // Inicializar conjuntos siguientes para cada no terminal
        for (NoTerminal nt : this.noTerminales) {
            siguientes.put(nt.getNombre(), new HashSet<>());
        }

        // Asignar el símbolo de fin de cadena ($) al símbolo inicial
        siguientes.get(this.getSimbInicial()).add("$");

        // Primer recorrido: agregar terminales que siguen a no terminales directamente
        for (Produccion pr : this.getProducciones()) {
            List<Simbolo> consecuente = pr.getConsec();
            for (int i = 0; i < consecuente.size() - 1; i++) {
                if (isNoTerminal(consecuente.get(i).getNombre()) && isTerminal(consecuente.get(i + 1).getNombre())) {
                    siguientes.get(consecuente.get(i).getNombre()).add(consecuente.get(i + 1).getNombre());
                }
            }
        }

        // Resolver dependencias y agregar conjuntos siguientes de no terminales
        boolean cambios;
        do {
            cambios = false;
            for (Produccion pr : this.getProducciones()) {
                List<Simbolo> consecuente = pr.getConsec();
                String antecedente = pr.getAntec().getSimboloNT().getNombre();
                for (int i = 0; i < consecuente.size(); i++) {
                    if (isNoTerminal(consecuente.get(i).getNombre())) {
                        Set<String> conjSig = siguientes.get(consecuente.get(i).getNombre());
                        int sizeAntes = conjSig.size();

                        // Agregar siguientes del antecedente si es el último símbolo o si el siguiente símbolo puede derivar en épsilon
                        if (i == consecuente.size() - 1 || (i < consecuente.size() - 1 && puedeDerivarEnEpsilon(consecuente.get(i + 1)))) {
                            conjSig.addAll(siguientes.get(antecedente));
                        }

                        // Agregar primeros del siguiente símbolo si es no terminal
                        if (i < consecuente.size() - 1 && isNoTerminal(consecuente.get(i + 1).getNombre())) {
                            conjSig.addAll(getPrimerosSinEpsilon(consecuente.get(i + 1).getNombre()));
                        }

                        if (conjSig.size() > sizeAntes) {
                            cambios = true;
                        }
                    }
                }
            }
        } while (cambios);

        // Actualizar los conjuntos siguientes en los no terminales
        for (NoTerminal nt : this.noTerminales) {
            Set<String> conjuntoSiguiente = siguientes.get(nt.getNombre());
            ObservableList<Terminal> listaSiguiente = FXCollections.observableArrayList();
            for (String simbolo : conjuntoSiguiente) {
                listaSiguiente.add(new Terminal(simbolo, simbolo));
            }
            nt.setSiguientes(listaSiguiente);
        }
    }

    private boolean puedeDerivarEnEpsilon(Simbolo simbolo) {
        if (isTerminal(simbolo.getNombre())) {
            return false;
        }
        for (Produccion pr : this.getProducciones()) {
            if (pr.getAntec().getSimboloNT().getNombre().equals(simbolo.getNombre()) &&
                    pr.getConsec().get(0).getNombre().equals("\u03b5")) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getPrimerosSinEpsilon(String nombreNoTerminal) {
        Set<String> primerosSinEpsilon = new HashSet<>();
        for (NoTerminal nt : this.noTerminales) {
            if (nt.getNombre().equals(nombreNoTerminal)) {
                for (Terminal t : nt.getPrimeros()) {
                    if (!t.getNombre().equals("\u03b5")) {
                        primerosSinEpsilon.add(t.getNombre());
                    }
                }
                break;
            }
        }
        return primerosSinEpsilon;
    }

    public String getProduccion(NoTerminal nt, String terminal) {
        List<String> reglas = new ArrayList<>();

        for (Produccion pr : this.getProducciones()) {
            if (pr.getAntec().getSimboloNT().getNombre().equals(nt.getNombre())) {
                // 1️⃣ Verificar si el terminal está en el conjunto Primero del consecuente
                Simbolo primerSimbolo = pr.getConsec().isEmpty() ? null : pr.getConsec().get(0);

                if (primerSimbolo != null && primerSimbolo.getNombre().equals(terminal)) {
                    reglas.add(pr.toString());
                }
                // 2️⃣ Si el primer símbolo es ε, agregarlo si el terminal está en Siguiente
                else if (primerSimbolo != null && primerSimbolo.getNombre().equals("ε")) {
                    if (nt.getSiguientes().stream().anyMatch(t -> t.getNombre().equals(terminal))) {
                        reglas.add(pr.toString());
                    }
                }
                // 3️⃣ Si el terminal está en Primero del No Terminal
                else if (nt.getPrimeros().stream().anyMatch(t -> t.getNombre().equals(terminal))) {
                    reglas.add(pr.toString());
                }
            }
        }

        return reglas.isEmpty() ? null : String.join(", ", reglas);
    }

    public List<String> getProduccionesPorNoTerminalYTerminal(NoTerminal nt, Terminal t) {
        List<String> reglas = new ArrayList<>();

        for (Produccion pr : this.getProducciones()) {
            if (pr.getAntec().getSimboloNT().getNombre().equals(nt.getNombre())) {
                if (pr.getConsec().isEmpty()) continue; // Producción vacía

                Simbolo primerSimbolo = pr.getConsec().get(0);

                // 1. Si el primer símbolo es el terminal que buscamos
                if (primerSimbolo.getNombre().equals(t.getNombre())) {
                    reglas.add(pr.toString());
                }
                // 2. Si el primer símbolo es épsilon y el terminal está en SIGUIENTE
                else if (primerSimbolo.getNombre().equals("ε") && 
                         nt.getSiguientes().stream().anyMatch(s -> s.getNombre().equals(t.getNombre()))) {
                    reglas.add(pr.toString());
                }
                // 3. Si el primer símbolo es un no terminal y el terminal está en su conjunto PRIMERO
                else if (isNoTerminal(primerSimbolo.getNombre())) {
                    NoTerminal primerNT = this.noTerminales.stream()
                        .filter(n -> n.getNombre().equals(primerSimbolo.getNombre()))
                        .findFirst()
                        .orElse(null);
                    
                    if (primerNT != null && primerNT.getPrimeros().stream()
                            .anyMatch(term -> term.getNombre().equals(t.getNombre()))) {
                        reglas.add(pr.toString());
                    }
                }
            }
        }

        return reglas;
    }

    public List<String> getProduccionesPorNoTerminalYTerminal(String noTerminal, String terminal) {
        // Buscar el no terminal y el terminal en las listas correspondientes
        NoTerminal nt = this.noTerminales.stream()
                         .filter(n -> n.getNombre().equals(noTerminal))
                         .findFirst()
                         .orElse(null);
    
        Terminal t = this.terminales.stream()
                         .filter(term -> term.getNombre().equals(terminal))
                         .findFirst()
                         .orElse(null);
    
        // Si no se encuentran, devolver una lista vacía
        if (nt == null || t == null) {
            return Collections.emptyList();
        }
    
        // Llamar al método existente con los objetos encontrados
        return getProduccionesPorNoTerminalYTerminal(nt, t);
    }

    public Set<String> getFollow(String noTerminal) {
        // Buscar el no terminal en la lista de no terminales
        for (NoTerminal nt : this.getNoTerminales()) {
            if (nt.getNombre().equals(noTerminal)) {
                // Convertir ObservableList<Terminal> a Set<String>
                return nt.getSiguientes().stream()
                         .map(Terminal::getNombre) // Obtener el nombre de cada terminal
                         .collect(Collectors.toSet()); // Convertir a Set<String>
            }
        }
        return Collections.emptySet(); // Devuelve un conjunto vacío si no se encuentra el no terminal
    }
    
    /**
     * Agrega la tabla predictiva del Paso 5 al PDF.
     */
    private void agregarTablaPredictivaAlPDF(Document document, TablaPredictiva tablaPredictiva, ResourceBundle bundle, BaseFont bf, Font contenido) {
        try {
            // Obtener los datos de la tabla predictiva (Paso 5)
            List<FilaTablaPredictiva> filas = tablaPredictiva.getFilas();
            if (filas == null || filas.isEmpty()) {
                return;
            }
            
            // Obtener los terminales de la gramática para las columnas
            List<Terminal> terminales = this.getTerminales();
            if (terminales.isEmpty()) {
                return;
            }
            
            // Crear tabla PDF con columnas: Símbolo + Terminales
            int numColumnas = terminales.size() + 1; // +1 para la columna del símbolo
            PdfPTable tabla = new PdfPTable(numColumnas);
            tabla.setWidthPercentage(100);
            
            // Configurar fuentes para encabezados
            Font encabezado = new Font(bf, 8, Font.BOLD);
            encabezado.setColor(new BaseColor(33, 77, 72));
            
            // Agregar encabezado de la columna de Símbolos
            PdfPCell celdaSimbolo = new PdfPCell(new Phrase("Símbolo", encabezado));
            celdaSimbolo.setBackgroundColor(new BaseColor(240, 240, 240));
            celdaSimbolo.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            celdaSimbolo.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            celdaSimbolo.setPadding(3);
            celdaSimbolo.setMinimumHeight(20);
            tabla.addCell(celdaSimbolo);
            
            // Agregar encabezados de terminales
            for (Terminal terminal : terminales) {
                PdfPCell celdaTerminal = new PdfPCell(new Phrase(terminal.getNombre(), encabezado));
                celdaTerminal.setBackgroundColor(new BaseColor(240, 240, 240));
                celdaTerminal.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                celdaTerminal.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
                celdaTerminal.setPadding(3);
                celdaTerminal.setMinimumHeight(20);
                tabla.addCell(celdaTerminal);
            }
            
            // Agregar TODAS las filas de datos (no terminales Y terminales)
            for (FilaTablaPredictiva fila : filas) {
                // Celda del símbolo (no terminal o terminal)
                String simbolo = fila.getSimbolo();
                PdfPCell celdaSim = new PdfPCell(new Phrase(simbolo, contenido));
                celdaSim.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                celdaSim.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
                celdaSim.setPadding(3);
                celdaSim.setMinimumHeight(20);
                
                // Color de fondo según el tipo de símbolo
                if (fila.getEsTerminal()) {
                    celdaSim.setBackgroundColor(new BaseColor(255, 240, 240)); // Rojo muy claro para terminales
                } else {
                    celdaSim.setBackgroundColor(new BaseColor(240, 255, 240)); // Verde muy claro para no terminales
                }
                tabla.addCell(celdaSim);
                
                // Celdas de las producciones/funciones de error para cada terminal
                for (Terminal terminal : terminales) {
                    String valor = fila.getValor(terminal.getNombre()).get();
                    String textoCelda = (valor != null && !valor.isEmpty()) ? valor : "";
                    
                    PdfPCell celdaDato = new PdfPCell(new Phrase(textoCelda, contenido));
                    celdaDato.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                    celdaDato.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
                    celdaDato.setPadding(3);
                    celdaDato.setMinimumHeight(20);
                    
                    // Colores según el tipo de contenido
                    if (textoCelda.contains("→")) {
                        celdaDato.setBackgroundColor(new BaseColor(220, 255, 220)); // Verde claro para producciones
                    } else if (textoCelda.startsWith("ε_")) {
                        celdaDato.setBackgroundColor(new BaseColor(255, 255, 200)); // Amarillo claro para épsilon
                    } else if (textoCelda.matches("\\d+")) {
                        celdaDato.setBackgroundColor(new BaseColor(255, 220, 220)); // Rojo claro para funciones de error
                    } else if (!textoCelda.isEmpty()) {
                        celdaDato.setBackgroundColor(new BaseColor(255, 240, 255)); // Magenta claro para otros valores
                    }
                    
                    tabla.addCell(celdaDato);
                }
            }
            
            // Agregar la tabla al documento
            document.add(tabla);
            
        } catch (Exception e) {
            // Si hay error, agregar un mensaje simple
            try {
                Paragraph errorTabla = new Paragraph("    • Error al generar tabla predictiva detallada", contenido);
                errorTabla.setIndentationLeft(20);
                document.add(errorTabla);
            } catch (DocumentException ex) {
                // Ignorar error al agregar mensaje de error
            }
        }
    }
    
    /**
     * Obtiene la descripción de una función de error en el idioma correspondiente.
     */
    private String getDescripcionFuncionError(FuncionError fe, ResourceBundle bundle) {
        if (fe == null) return "";
        
        int accion = fe.getAccion();
        Terminal simbolo = fe.getSimbolo();
        
        switch (accion) {
            case FuncionError.TERMINAR_ANALISIS:
                return bundle.getString("funcion.error.terminar");
            case FuncionError.BORRAR_ENTRADA:
                return bundle.getString("funcion.error.borrar.entrada");
            case FuncionError.INSERTAR_ENTRADA:
                if (simbolo != null) {
                    return bundle.getString("funcion.error.insertar.entrada") + ": " + simbolo.getNombre();
                } else {
                    return bundle.getString("funcion.error.insertar.entrada");
                }
            case FuncionError.MODIFICAR_ENTRADA:
                if (simbolo != null) {
                    return bundle.getString("funcion.error.modificar.entrada") + ": " + simbolo.getNombre();
                } else {
                    return bundle.getString("funcion.error.modificar.entrada");
                }
            case FuncionError.INSERTAR_PILA:
                if (simbolo != null) {
                    return bundle.getString("funcion.error.insertar.pila") + ": " + simbolo.getNombre();
                } else {
                    return bundle.getString("funcion.error.insertar.pila");
                }
            case FuncionError.BORRAR_PILA:
                return bundle.getString("funcion.error.borrar.pila");
            case FuncionError.MODIFICAR_PILA:
                if (simbolo != null) {
                    return bundle.getString("funcion.error.modificar.pila") + ": " + simbolo.getNombre();
                } else {
                    return bundle.getString("funcion.error.modificar.pila");
                }
            default:
                return fe.toString();
        }
    }

}
