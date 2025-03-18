package gramatica;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;


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
        // Solo genera el informe si la gramática está validada (estado==1)
        if (this.getEstado() == 1) {
            try {
                // Configuración de la fuente y del documento PDF
                String fontPath = "fonts/arial.ttf";
                Document document = new Document(PageSize.LETTER, 45, 45, 54, 45);
                Image imagen = Image.getInstance(Objects.requireNonNull(getClass().getResource("/resources/logo2Antes.png")).toExternalForm());
                imagen.setAlignment(Image.ALIGN_CENTER);
                imagen.scalePercent(40);

                LineSeparator ls = new LineSeparator();
                BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                Font titulo = new Font(bf, 21, Font.BOLD);
                Font font2 = new Font(bf, 15, Font.BOLD);
                Font font3 = new Font(bf, 12);
                BaseColor claro = new BaseColor(63, 171, 160);

                titulo.setColor(33, 77, 72);
                font2.setColor(BaseColor.BLACK);

                ls.setLineWidth(1);
                ls.setLineColor(claro);

                // Crear párrafos para el informe
                Paragraph parrafoTitulo = new Paragraph(" INFORME DE LA GRAMÁTICA ", titulo);
                parrafoTitulo.setAlignment(Paragraph.ALIGN_CENTER);

                Paragraph parrafoNombre = new Paragraph("\n Nombre de la gramática: ", font2);
                parrafoNombre.add(new Paragraph("    " + this.getNombre() + "\n", font3));

                Paragraph parrafoDescripcion = new Paragraph("\n Descripción de la gramática: ", font2);
                parrafoDescripcion.add(new Paragraph("    " + this.getDescripcion() + "\n", font3));

                // Símbolos terminales: se itera sobre la lista de terminales (ObservableList<String>)
                Paragraph parrafoTerminales = new Paragraph("\n Símbolos terminales: ", font2);
                ObservableList<String> termModel = this.getTerminalesModel(); // Método que devuelve ObservableList<String>
                for (String t : termModel) {
                    parrafoTerminales.add(new Paragraph("    " + t, font3));
                }

                // Símbolos no terminales
                Paragraph parrafoNoTerminales = new Paragraph("\n Símbolos no terminales: ", font2);
                ObservableList<String> noTermModel = this.getNoTerminalesModel();
                for (String nt : noTermModel) {
                    parrafoNoTerminales.add(new Paragraph("    " + nt, font3));
                }

                // Símbolo inicial
                Paragraph parrafoSimboloInicial = new Paragraph("\n Símbolo inicial de la gramática: ", font2);
                parrafoSimboloInicial.add(new Paragraph("    " + this.getSimbInicial(), font3));

                // Producciones: se prepara una nueva lista para formatear la salida
                Paragraph parrafoProducciones = new Paragraph("\n Producciones de la gramática: ", font2);
                ObservableList<String> prodModel = this.getProduccionesModel();
                // Crear una lista auxiliar para añadir cabecera y cierre
                ObservableList<String> prodFormateadas = FXCollections.observableArrayList();
                prodFormateadas.add("P {");
                int index = 1;
                for (String prod : prodModel) {
                    prodFormateadas.add("   " + (index++) + ")   " + prod);
                }
                prodFormateadas.add("}");
                for (String prodStr : prodFormateadas) {
                    parrafoProducciones.add(new Paragraph("    " + prodStr, font3));
                }

                // Crear el PdfWriter para escribir el documento en el fichero
                try {
                    PdfWriter.getInstance(document, new FileOutputStream(fichero));
                } catch (DocumentException | FileNotFoundException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }

                document.open();
                try {
                    document.add(imagen);
                    document.add(parrafoTitulo);
                    document.add(new Chunk(ls));
                    document.add(parrafoNombre);
                    document.add(parrafoDescripcion);
                    document.add(parrafoTerminales);
                    document.add(parrafoNoTerminales);
                    document.add(parrafoSimboloInicial);
                    document.add(parrafoProducciones);
                } catch (DocumentException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }

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
        this.tpredictiva = new TablaPredictiva();
        this.tpredictiva.construir(this);
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

                if (primerSimbolo.getNombre().equals(t.getNombre()) ||
                        (primerSimbolo.getNombre().equals("ε") && nt.getSiguientes().stream().anyMatch(s -> s.getNombre().equals(t.getNombre()))) ||
                        nt.getPrimeros().stream().anyMatch(s -> s.getNombre().equals(t.getNombre()))) {

                    reglas.add(pr.toString());
                }
            }
        }

        return reglas;
    }


}
