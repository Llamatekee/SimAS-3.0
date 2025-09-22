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
import java.text.MessageFormat;
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
    private StringProperty archivoFuente = new SimpleStringProperty(); // Nombre del archivo fuente (sin extensión)
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

    // Clase interna para representar nodos del árbol sintáctico
    private static class NodoArbol {
        String valor;
        List<NodoArbol> hijos = new ArrayList<>();

        NodoArbol(String valor) {
            this.valor = valor;
        }
    }

    /**
     * Resuelve una ruta válida hacia la fuente usada en los PDFs tanto en desarrollo
     * como dentro de un bundle .app (jpackage). Intenta, en este orden:
     * 1) Cargar desde recursos del classpath (si estuviera embebido)
     * 2) Cargar desde el directorio del JAR (Contents/app/fonts/...)
     * 3) Usar la ruta relativa de trabajo (fonts/...)
     */
    private static String resolveFontPath() {
        final String relativeFont = "fonts/arial.ttf";
        try {
            java.net.URL res = Gramatica.class.getResource("/fonts/arial.ttf");
            if (res != null) {
                try (java.io.InputStream in = Gramatica.class.getResourceAsStream("/fonts/arial.ttf")) {
                    if (in != null) {
                        java.nio.file.Path temp = java.nio.file.Files.createTempFile("arial_", ".ttf");
                        java.nio.file.Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        temp.toFile().deleteOnExit();
                        return temp.toString();
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            java.io.File jar = new java.io.File(Gramatica.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File font = new java.io.File(jar.getParentFile(), relativeFont);
            if (font.exists()) return font.getAbsolutePath();
        } catch (Exception ignored) {}
        return relativeFont;
    }

    // Constructor con parámetros
    public Gramatica(String nombre, String descripcion) {
        this.nombre.set(nombre);
        this.descripcion.set(descripcion);
        this.archivoFuente.set("untitled"); // Valor por defecto para gramáticas creadas desde cero
    }

    // Constructor con parámetros
    public Gramatica(Gramatica gramatica) {
        this.nombre.set(gramatica.getNombre());
        this.descripcion.set(gramatica.getDescripcion());
        this.archivoFuente.set(gramatica.getArchivoFuente());
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
        this.archivoFuente.set("untitled"); // Valor por defecto para gramáticas creadas desde cero
    }


    /**
     * Genera el nombre del archivo PDF según el tipo de informe y el idioma actual
     * @param tipoInforme Tipo de informe: "editor", "simulador", "simulacion"
     * @param bundle ResourceBundle para internacionalización
     * @return Nombre del archivo PDF
     */
    public String generarNombreArchivoPDF(String tipoInforme, ResourceBundle bundle) {
        String baseNombre = getArchivoFuente();

        // Si no hay bundle, usar valores por defecto en español
        if (bundle == null) {
            bundle = new ResourceBundle() {
                @Override
                protected Object handleGetObject(String key) {
                    switch (key) {
                        case "archivo.nombre.untitled": return "untitled";
                        case "archivo.nombre.simulador": return "simulador";
                        case "archivo.nombre.simulacion": return "simulacion";
                        default: return key;
                    }
                }

                @Override
                public Enumeration<String> getKeys() {
                    return Collections.enumeration(Arrays.asList(
                        "archivo.nombre.untitled", "archivo.nombre.simulador", "archivo.nombre.simulacion"
                    ));
                }
            };
        }

        // Si el nombre base es "untitled", traducirlo según el idioma
        if ("untitled".equals(baseNombre)) {
            baseNombre = bundle.getString("archivo.nombre.untitled");
        }

        switch (tipoInforme.toLowerCase()) {
            case "editor":
                return baseNombre + ".pdf";
            case "simulador":
                return baseNombre + "_" + bundle.getString("archivo.nombre.simulador") + ".pdf";
            case "simulacion":
                return baseNombre + "_" + bundle.getString("archivo.nombre.simulacion") + ".pdf";
            default:
                return baseNombre + ".pdf";
        }
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

    public String getArchivoFuente() {
        return archivoFuente.get();
    }

    public void setArchivoFuente(String archivoFuente) {
        this.archivoFuente.set(archivoFuente);
    }

    public StringProperty archivoFuenteProperty() {
        return archivoFuente;
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
        // Quitar numeración opcional tipo "1. " al inicio del antecedente
        antecedente = antecedente.replaceFirst("^\\d+\\.\\s*", "");
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

        // Extraer el nombre del archivo sin extensión
        String nombreArchivo = file.getName();
        if (nombreArchivo.contains(".")) {
            nombreArchivo = nombreArchivo.substring(0, nombreArchivo.lastIndexOf('.'));
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
            // Guardar el nombre del archivo fuente
            gramatica.setArchivoFuente(nombreArchivo);
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
        // Intentar usar el bundle por defecto del sistema si existe
        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle("utils.messages", java.util.Locale.getDefault());
        } catch (Exception ignored) {}
        return validarGramatica(bundle);
    }

    public ObservableList<String> validarGramatica(ResourceBundle bundle) {
        // Usamos ObservableList para que, si se requiere, se pueda enlazar con la UI
        ObservableList<String> mensajesError = FXCollections.observableArrayList();
        // Variables locales para la validación
        ObservableList<Simbolo> conjSimbolos;
        this.setEstado(1);

        // Helpers i18n
        final ResourceBundle localBundle = bundle;
        java.util.function.Function<String, String> tr = key -> {
            if (localBundle != null) {
                try { if (localBundle.containsKey(key)) return localBundle.getString(key); } catch (Exception ignored) {}
            }
            return key; // fallback
        };
        java.util.function.BiFunction<String, Object[], String> fmt = (key, args) -> {
            String pattern = tr.apply(key);
            return MessageFormat.format(pattern, args);
        };

        // Evitar duplicados (preserva orden)
        java.util.Set<String> erroresUnicos = new java.util.LinkedHashSet<>();

        // Validar existencia de producciones
        if (this.producciones.isEmpty()) {
            this.setEstado(-1);
            erroresUnicos.add(tr.apply("validation.no_productions.title") + "\n" + tr.apply("validation.no_productions.desc"));
        }

        // Validar existencia de símbolos terminales
        if (this.terminales.isEmpty()) {
            this.setEstado(-1);
            erroresUnicos.add(tr.apply("validation.no_terminals.title") + "\n" + tr.apply("validation.no_terminals.desc"));
        }

        // Validar existencia de símbolos no terminales
        if (this.noTerminales.isEmpty()) {
            this.setEstado(-1);
            erroresUnicos.add(tr.apply("validation.no_nonterminals.title") + "\n" + tr.apply("validation.no_nonterminals.desc"));
        }

        // Validar asignación del símbolo inicial
        if (this.getSimbInicial() == null) {
            this.setEstado(-1);
            erroresUnicos.add(tr.apply("validation.no_start.title") + "\n" + tr.apply("validation.no_start.desc"));
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
                erroresUnicos.add(tr.apply("validation.terminal_not_used.title") + "\n" +
                        fmt.apply("validation.terminal_not_used.desc", new Object[]{t.getNombre()}));
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
                erroresUnicos.add(tr.apply("validation.nonterminal_not_used_consequent.title") + "\n" +
                        fmt.apply("validation.nonterminal_not_used_consequent.desc", new Object[]{nt.getNombre()}));
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
                erroresUnicos.add(tr.apply("validation.nonterminal_not_used_antecedent.title") + "\n" +
                        fmt.apply("validation.nonterminal_not_used_antecedent.desc", new Object[]{antecProd.getSimboloNT().getNombre()}));
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
                    erroresUnicos.add(tr.apply("validation.consequent_symbol_not_declared.title") + "\n" +
                            fmt.apply("validation.consequent_symbol_not_declared.desc", new Object[]{s.getNombre()}));
                }
            }
        }
        mensajesError.setAll(erroresUnicos);
        return mensajesError;
    }


    public Boolean generarInforme(String fichero) throws DocumentException {
        // Método original para compatibilidad - usa español por defecto
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("utils.messages", new java.util.Locale("es"));
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
                            case "informe.profesional.generado": return "Documento generado el";
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
                            "informe.documento.generado", "informe.pagina",
                            "informe.profesional.generado"
                        ));
                    }
                };
            }
            try {
                // Configuración inicial del documento - ESTILOS PROFESIONALES IDÉNTICOS
                String fontPath = resolveFontPath();
                Document document = new Document(PageSize.A4, 50, 50, 80, 50);

                // Crear el PdfWriter con gestión avanzada de páginas
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fichero));

                // Configurar esquema de colores profesional - IDÉNTICOS a los métodos profesionales
                BaseColor colorPrimario = new BaseColor(41, 128, 185);     // Azul profesional
                BaseColor colorSecundario = new BaseColor(52, 152, 219);   // Azul claro
                BaseColor colorAcento = new BaseColor(230, 126, 34);       // Naranja
                BaseColor colorNeutro = new BaseColor(149, 165, 166);      // Gris neutro

                // Fuentes tipográficas profesionales - IDÉNTICAS
                BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                BaseFont bfMono = BaseFont.createFont(BaseFont.COURIER, BaseFont.CP1252, BaseFont.EMBEDDED);

                Font tituloPortada = new Font(bf, 32, Font.BOLD);
                Font subtituloPortada = new Font(bf, 20, Font.BOLD);
                Font tituloSeccion = new Font(bf, 18, Font.BOLD);
                Font subtituloSeccion = new Font(bf, 14, Font.BOLD);
                Font contenidoNormal = new Font(bf, 11);
                Font contenidoMono = new Font(bfMono, 10);
                Font piePagina = new Font(bf, 9, Font.ITALIC);

                tituloPortada.setColor(colorPrimario);
                subtituloPortada.setColor(colorSecundario);
                tituloSeccion.setColor(colorPrimario);
                subtituloSeccion.setColor(colorAcento);
                contenidoNormal.setColor(BaseColor.BLACK);
                piePagina.setColor(colorNeutro);

                // Configurar separadores visuales - IDÉNTICOS
                LineSeparator separadorPrincipal = new LineSeparator();
                separadorPrincipal.setLineWidth(3);
                separadorPrincipal.setLineColor(colorPrimario);

                LineSeparator separadorSecundario = new LineSeparator();
                separadorSecundario.setLineWidth(1);
                separadorSecundario.setLineColor(colorNeutro);

                // Event handler para encabezados y pies de página - IDÉNTICO
                final ResourceBundle finalBundle = bundle;
                final BaseFont finalBf = bf;
                final BaseColor finalColorNeutro = colorNeutro;

                writer.setPageEvent(new PdfPageEventHelper() {
                    @Override
                    public void onEndPage(PdfWriter writer, Document document) {
                        try {
                            // Pie de página con numeración
                            Font fontPie = new Font(finalBf, 9, Font.ITALIC);
                            fontPie.setColor(finalColorNeutro);

                            ColumnText.showTextAligned(writer.getDirectContent(),
                                Paragraph.ALIGN_CENTER,
                                new Phrase(String.format("%s %d", finalBundle.getString("informe.pagina"), writer.getPageNumber()), fontPie),
                                (document.right() - document.left()) / 2 + document.leftMargin(),
                                document.bottom() - 15, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onStartPage(PdfWriter writer, Document document) {
                        // Encabezado eliminado - no mostrar título en cada página
                    }
                });

                document.open();

                // ========================================
                // PÁGINA 1: PORTADA PROFESIONAL - IDÉNTICA
                // ========================================
                crearPortadaProfesional(document, bf, tituloPortada, subtituloPortada, separadorPrincipal, bundle, colorPrimario, colorSecundario);

                // ========================================
                // PÁGINA 2: CONTENIDO - SOLO GRAMÁTICA ORIGINAL
                // ========================================
                document.newPage();

                // ========================================
                // SECCIÓN ÚNICA: GRAMÁTICA ORIGINAL
                // ========================================
                Paragraph tituloGramaticaOriginal = new Paragraph(bundle.getString("informe.editor.gramatica.original"), tituloSeccion);
                document.add(tituloGramaticaOriginal);
                document.add(new Chunk(separadorSecundario));
                document.add(new Paragraph(" ", new Font(bf, 10)));

                // Información básica de la gramática usando el método profesional
                agregarInformacionGramatica(document, bf, subtituloSeccion, contenidoNormal, contenidoMono,
                                          bundle, this, "original", colorPrimario, colorSecundario);

                // ========================================
                // INFORMACIÓN FINAL - IDÉNTICA AL INFORME DE SIMULACIÓN
                // ========================================
                document.add(new Paragraph(" ", new Font(bf, 25)));

                // Información adicional
                Font infoFont = new Font(bf, 10);
                infoFont.setColor(BaseColor.GRAY);
                Paragraph infoAdicional = new Paragraph(bundle.getString("informe.profesional.conclusion.creditos"), infoFont);
                infoAdicional.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(infoAdicional);

                Paragraph fechaConclusion = new Paragraph(bundle.getString("informe.profesional.fecha") + " " +
                    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), infoFont);
                fechaConclusion.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(fechaConclusion);

                document.close();

            } catch (BadElementException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error de elemento en PDF", ex);
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error de E/S en PDF", ex);
            } catch (Exception ex) { // Para capturar cualquier otra excepción
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error general en generación de PDF", ex);
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Genera un informe PDF completo del simulador siguiendo el mismo formato profesional
     * pero sin las secciones: resumen ejecutivo, información de simulación, historial de pasos,
     * derivación, árbol sintáctico y conclusión.
     */
    public Boolean generarInformeSimulador(String fichero, Gramatica gramaticaOriginal, TablaPredictiva tablaPredictiva, 
                                         List<FuncionError> funcionesError, ResourceBundle bundle) throws DocumentException {
        try {
            // Configuración inicial del documento
            String fontPath = resolveFontPath();
            Document document = new Document(PageSize.A4, 50, 50, 80, 50);
            
            // Crear el PdfWriter con gestión avanzada de páginas
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fichero));
            
            // Configurar esquema de colores profesional
            BaseColor colorPrimario = new BaseColor(41, 128, 185);     // Azul profesional
            BaseColor colorSecundario = new BaseColor(52, 152, 219);   // Azul claro
            BaseColor colorAcento = new BaseColor(230, 126, 34);       // Naranja
            BaseColor colorNeutro = new BaseColor(149, 165, 166);      // Gris neutro
            BaseColor colorFondoCabecera = new BaseColor(236, 240, 241); // Gris muy claro

            // Fuentes tipográficas profesionales
            BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfMono = BaseFont.createFont(BaseFont.COURIER, BaseFont.CP1252, BaseFont.EMBEDDED);

            Font tituloPortada = new Font(bf, 32, Font.BOLD);
            Font subtituloPortada = new Font(bf, 20, Font.BOLD);
            Font tituloSeccion = new Font(bf, 18, Font.BOLD);
            Font subtituloSeccion = new Font(bf, 14, Font.BOLD);
            Font contenidoNormal = new Font(bf, 11);
            Font contenidoMono = new Font(bfMono, 10);
            Font piePagina = new Font(bf, 9, Font.ITALIC);

            tituloPortada.setColor(colorPrimario);
            subtituloPortada.setColor(colorSecundario);
            tituloSeccion.setColor(colorPrimario);
            subtituloSeccion.setColor(colorAcento);
            contenidoNormal.setColor(BaseColor.BLACK);
            piePagina.setColor(colorNeutro);

            // Configurar separadores visuales
            LineSeparator separadorPrincipal = new LineSeparator();
            separadorPrincipal.setLineWidth(3);
            separadorPrincipal.setLineColor(colorPrimario);

            LineSeparator separadorSecundario = new LineSeparator();
            separadorSecundario.setLineWidth(1);
            separadorSecundario.setLineColor(colorNeutro);

            // Event handler para encabezados y pies de página
            final ResourceBundle finalBundle = bundle;
            final BaseFont finalBf = bf;
            final BaseColor finalColorNeutro = colorNeutro;

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        // Pie de página con numeración
                        Font fontPie = new Font(finalBf, 9, Font.ITALIC);
                        fontPie.setColor(finalColorNeutro);

                        ColumnText.showTextAligned(writer.getDirectContent(), 
                            Paragraph.ALIGN_CENTER, 
                            new Phrase(String.format("%s %d", finalBundle.getString("informe.pagina"), writer.getPageNumber()), fontPie),
                            (document.right() - document.left()) / 2 + document.leftMargin(), 
                            document.bottom() - 15, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStartPage(PdfWriter writer, Document document) {
                    // Encabezado eliminado - no mostrar título en cada página
                }
            });

            document.open();
            
            // Determinar qué secciones estarán presentes
            boolean hasFuncionesError = funcionesError != null && !funcionesError.isEmpty();
            boolean hasTablaPredictiva = tablaPredictiva != null;

            // ========================================
            // PÁGINA 1: PORTADA PROFESIONAL
            // ========================================
            crearPortadaProfesional(document, bf, tituloPortada, subtituloPortada, separadorPrincipal, bundle, colorPrimario, colorSecundario);

            // ========================================
            // PÁGINA 2: ÍNDICE AUTOMÁTICO
            // ========================================
            document.newPage();
            crearIndiceSimulador(document, bf, tituloSeccion, contenidoNormal, separadorSecundario, bundle, hasFuncionesError, hasTablaPredictiva);

            // ========================================
            // CONTENIDO DETALLADO (SIN SECCIONES EXCLUIDAS)
            // ========================================
            document.newPage();

            // ========================================
            // SECCIÓN 1: GRAMÁTICA ORIGINAL
            // ========================================
            Paragraph tituloGramaticaOriginal = new Paragraph("1. " + bundle.getString("informe.simulador.gramatica.original").toUpperCase(), tituloSeccion);
            document.add(tituloGramaticaOriginal);
            document.add(new Chunk(separadorSecundario));
            document.add(new Paragraph(" ", new Font(bf, 10)));

            // Información básica
            agregarInformacionGramatica(document, bf, subtituloSeccion, contenidoNormal, contenidoMono,
                                      bundle, gramaticaOriginal, "original", colorPrimario, colorSecundario);

            // ========================================
            // SECCIÓN 2: GRAMÁTICA MODIFICADA
            // ========================================
            document.newPage();
            Paragraph tituloGramaticaModificada = new Paragraph("2. " + bundle.getString("informe.simulador.gramatica.modificada").toUpperCase(), tituloSeccion);
            document.add(tituloGramaticaModificada);
            document.add(new Chunk(separadorSecundario));
            document.add(new Paragraph(" ", new Font(bf, 10)));

            agregarInformacionGramatica(document, bf, subtituloSeccion, contenidoNormal, contenidoMono,
                                      bundle, this, "modificada", colorPrimario, colorSecundario);

            // ========================================
            // SECCIÓN 3: FUNCIONES DE ERROR
            // ========================================
            int seccionNumero = 3;
            if (funcionesError != null && !funcionesError.isEmpty()) {
                document.newPage();
                Paragraph tituloFuncionesError = new Paragraph(seccionNumero + ". " + bundle.getString("informe.simulador.funciones.error").toUpperCase(), tituloSeccion);
                document.add(tituloFuncionesError);
                document.add(new Chunk(separadorSecundario));
                document.add(new Paragraph(" ", new Font(bf, 10)));

                agregarFuncionesError(document, bf, contenidoNormal, bundle, funcionesError,
                                    colorPrimario, colorSecundario, colorFondoCabecera, colorAcento);
                seccionNumero++;
            }

            // ========================================
            // SECCIÓN 4: TABLA PREDICTIVA
            // ========================================
            if (tablaPredictiva != null) {
            document.newPage();
                Paragraph tituloTablaPredictiva = new Paragraph(seccionNumero + ". " + bundle.getString("informe.simulador.tabla.predictiva").toUpperCase(), tituloSeccion);
                document.add(tituloTablaPredictiva);
                document.add(new Chunk(separadorSecundario));
                document.add(new Paragraph(" ", new Font(bf, 10)));

                agregarTablaPredictivaMejorada(document, tablaPredictiva, bundle, bf, contenidoNormal,
                                             colorPrimario, colorSecundario, colorFondoCabecera);
                seccionNumero++;
            }

            // ========================================
            // INFORMACIÓN FINAL - IDÉNTICA AL INFORME DE SIMULACIÓN
            // ========================================
            document.add(new Paragraph(" ", new Font(bf, 25)));

            // Información adicional
            Font infoFont = new Font(bf, 10);
            infoFont.setColor(BaseColor.GRAY);
            Paragraph infoAdicional = new Paragraph(bundle.getString("informe.profesional.conclusion.creditos"), infoFont);
            infoAdicional.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(infoAdicional);

            Paragraph fechaConclusion = new Paragraph(bundle.getString("informe.profesional.fecha") + " " +
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), infoFont);
            fechaConclusion.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(fechaConclusion);
            
            document.close();
            
        } catch (BadElementException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error de elemento en PDF", ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error de E/S en PDF", ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error general en generación de PDF", ex);
        }
        return true;
    }

        /**
     * Verifica si un símbolo es terminal
     */
    private boolean esTerminal(String simbolo) {
        return !this.getNoTerminalesModel().contains(simbolo);
    }

    /**
     * Construye el árbol sintáctico desde el historial de simulación
     */
    private NodoArbol construirArbolDesdeHistorial(List<HistorialPaso> historial) {
        // Creamos la raíz con el símbolo inicial
        NodoArbol raiz = new NodoArbol(this.getSimbInicial());

        // Crear una copia del historial para no modificar el original
        List<HistorialPaso> historialCopia = new ArrayList<>(historial);
        construirRecursivo(raiz, historialCopia);

        return raiz;
    }

    /**
     * Función recursiva para construir el árbol
     */
    private void construirRecursivo(NodoArbol nodo, List<HistorialPaso> pasos) {
        if (pasos.isEmpty()) {
            return;
        }

        // Buscar la producción que expande este nodo
        for (int i = 0; i < pasos.size(); i++) {
            HistorialPaso paso = pasos.get(i);
            String accion = paso.getAccion();

            // Si es una producción (contiene una flecha)
            if (accion.contains("→")) {
                // Eliminar el número de producción si existe
                String accionLimpia = accion.replaceAll("^\\d+\\.\\s*", "").trim();
                String[] partes = accionLimpia.split("→");
                String izquierda = partes[0].trim();
                String derecha = partes[1].trim();

                // Si esta producción corresponde al nodo actual
                if (izquierda.equals(nodo.valor)) {
                    // Dividir la parte derecha en símbolos
                    String[] simbolos = derecha.split("\\s+");

                    // Crear nodos hijos para cada símbolo
                    for (String simbolo : simbolos) {
                        if (!simbolo.isEmpty()) {
                            NodoArbol hijo = new NodoArbol(simbolo);
                            nodo.hijos.add(hijo);

                            // Si el hijo es no terminal, procesarlo recursivamente
                            if (!esTerminal(simbolo)) {
                                // Crear una nueva lista con los pasos restantes
                                List<HistorialPaso> pasosRestantes = new ArrayList<>(pasos.subList(i + 1, pasos.size()));
                                construirRecursivo(hijo, pasosRestantes);
                            }
                        }
                    }
                    break; // Solo procesar la primera producción que coincida
                }
            }
        }
    }

    /**
     * Genera código DOT desde el árbol sintáctico
     */
    private String generarDotDesdeArbol(NodoArbol raiz) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        sb.append("  node [shape=box, style=rounded, fontname=\"Arial\"];\n");
        sb.append("  edge [arrowhead=none];\n");

        // Usar un contador para generar IDs únicos de nodos
        int[] idCounter = {0};
        List<String> hojas = new ArrayList<>();
        generarDotRec(raiz, sb, idCounter, null, hojas);

        // Forzar que todas las hojas queden alineadas en el mismo nivel
        if (!hojas.isEmpty()) {
            sb.append("  { rank=same; ");
            for (String leafId : hojas) {
                sb.append(leafId).append("; ");
            }
            sb.append("}\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Función recursiva para generar código DOT
     */
    private void generarDotRec(NodoArbol nodo, StringBuilder sb, int[] id, String parentId, List<String> hojas) {
        String myId = "n" + id[0]++;
        sb.append(myId + " [label=\"" + nodo.valor.replace("\"", "\\\"") + "\"];\n");
        if (parentId != null) {
            sb.append(parentId + " -> " + myId + ";\n");
        }
        if (nodo.hijos == null || nodo.hijos.isEmpty()) {
            hojas.add(myId);
            return;
        }
        for (NodoArbol hijo : nodo.hijos) {
            generarDotRec(hijo, sb, id, myId, hojas);
        }
    }

    /**
     * Genera la imagen del árbol sintáctico y la añade al PDF
     */
    private void agregarImagenArbolAlPDF(Document document, List<HistorialPaso> historialPasos) {
        try {
            // Verificar si hay historial disponible
            if (historialPasos == null || historialPasos.isEmpty()) {
                return;
            }

            // Construir el árbol desde el historial
            NodoArbol raiz = construirArbolDesdeHistorial(historialPasos);

            // Generar código DOT
            String dotCode = generarDotDesdeArbol(raiz);

            // Crear archivos temporales
            java.nio.file.Path dotFile = java.nio.file.Files.createTempFile("arbol_pdf_", ".dot");
            java.nio.file.Path imgFile = java.nio.file.Files.createTempFile("arbol_pdf_", ".png");

            try {
                // Escribir código DOT al archivo
                java.nio.file.Files.write(dotFile, dotCode.getBytes());

                // Ejecutar Graphviz para generar la imagen (resolver 'dot' y extender PATH para ejecución desde Finder)
                String dotExec = utils.GraphvizUtils.resolveDotExecutable();
                ProcessBuilder pb = new ProcessBuilder(dotExec, "-Tpng", dotFile.toString(), "-o", imgFile.toString());
                String currentPath = System.getenv("PATH");
                pb.environment().put("PATH", utils.GraphvizUtils.extendedPathEnv(currentPath));
                Process process = pb.start();
                int exitCode = process.waitFor();

                if (exitCode == 0 && java.nio.file.Files.exists(imgFile)) {
                    // Crear imagen para el PDF usando iText
                    com.itextpdf.text.Image imagenArbol = com.itextpdf.text.Image.getInstance(imgFile.toString());

                    // Configurar la imagen para que quepa en el PDF
                    float maxWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
                    float maxHeight = 300; // Altura máxima fija

                    // Calcular escala para ajustar la imagen
                    float scaleX = maxWidth / imagenArbol.getWidth();
                    float scaleY = maxHeight / imagenArbol.getHeight();
                    float scale = Math.min(scaleX, scaleY);

                    imagenArbol.scalePercent(scale * 100);

                    // Centrar la imagen
                    imagenArbol.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);

                    // Añadir la imagen al documento
                    document.add(imagenArbol);
                }
            } finally {
                // Limpiar archivos temporales
                try {
                    java.nio.file.Files.deleteIfExists(dotFile);
                    java.nio.file.Files.deleteIfExists(imgFile);
                } catch (Exception e) {
                    // Ignorar errores al limpiar archivos temporales
                }
            }
        } catch (Exception e) {
            // Si hay algún error (GraphViz no disponible, etc.), simplemente no añadir la imagen
            // Esto es mejor que fallar toda la generación del PDF
            Logger.getLogger(getClass().getName()).log(Level.WARNING,
                "No se pudo generar la imagen del árbol sintáctico: " + e.getMessage());
        }
    }

    /**
     * Genera un informe PDF profesional y visualmente atractivo de la simulación
     * Incluye portada, índice, resumen ejecutivo, y contenido formateado con colores y estilos
     */
    public Boolean generarInformeSimulacionFinalProfesional(String fichero, Gramatica gramaticaOriginal, TablaPredictiva tablaPredictiva,
                                         List<FuncionError> funcionesError, ResourceBundle bundle, String cadenaEntrada,
                                         String estadoSimulacion, List<HistorialPaso> historialPasos) throws DocumentException {
        try {
            // Configuración inicial del documento
            String fontPath = resolveFontPath();
            Document document = new Document(PageSize.A4, 50, 50, 80, 50);

            // Crear el PdfWriter con gestión avanzada de páginas
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fichero));

            // Configurar esquema de colores profesional
            BaseColor colorPrimario = new BaseColor(41, 128, 185);     // Azul profesional
            BaseColor colorSecundario = new BaseColor(52, 152, 219);   // Azul claro
            BaseColor colorAcento = new BaseColor(230, 126, 34);       // Naranja
            BaseColor colorExito = new BaseColor(46, 204, 113);        // Verde éxito
            BaseColor colorError = new BaseColor(231, 76, 60);         // Rojo error
            BaseColor colorNeutro = new BaseColor(149, 165, 166);      // Gris neutro
            BaseColor colorFondoCabecera = new BaseColor(236, 240, 241); // Gris muy claro

            // Fuentes tipográficas profesionales
            BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfMono = BaseFont.createFont(BaseFont.COURIER, BaseFont.CP1252, BaseFont.EMBEDDED);

            Font tituloPortada = new Font(bf, 32, Font.BOLD);
            Font subtituloPortada = new Font(bf, 20, Font.BOLD);
            Font tituloSeccion = new Font(bf, 18, Font.BOLD);
            Font subtituloSeccion = new Font(bf, 14, Font.BOLD);
            Font contenidoNormal = new Font(bf, 11);
            Font contenidoMono = new Font(bfMono, 10);
            Font piePagina = new Font(bf, 9, Font.ITALIC);

            tituloPortada.setColor(colorPrimario);
            subtituloPortada.setColor(colorSecundario);
            tituloSeccion.setColor(colorPrimario);
            subtituloSeccion.setColor(colorAcento);
            contenidoNormal.setColor(BaseColor.BLACK);
            piePagina.setColor(colorNeutro);

            // Configurar separadores visuales
            LineSeparator separadorPrincipal = new LineSeparator();
            separadorPrincipal.setLineWidth(3);
            separadorPrincipal.setLineColor(colorPrimario);

            LineSeparator separadorSecundario = new LineSeparator();
            separadorSecundario.setLineWidth(1);
            separadorSecundario.setLineColor(colorNeutro);

            // Event handler para encabezados y pies de página
            final ResourceBundle finalBundle = bundle;
            final BaseFont finalBf = bf;
            final BaseColor finalColorNeutro = colorNeutro;

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        // Pie de página con numeración
                        Font fontPie = new Font(finalBf, 9, Font.ITALIC);
                        fontPie.setColor(finalColorNeutro);

                        ColumnText.showTextAligned(writer.getDirectContent(),
                            Paragraph.ALIGN_CENTER,
                            new Phrase(String.format("%s %d", finalBundle.getString("informe.pagina"), writer.getPageNumber()), fontPie),
                            (document.right() - document.left()) / 2 + document.leftMargin(),
                            document.bottom() - 15, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStartPage(PdfWriter writer, Document document) {
                    // Encabezado eliminado - no mostrar título en cada página
                }
            });

            document.open();

            // ========================================
            // PÁGINA 1: PORTADA PROFESIONAL
            // ========================================
            crearPortadaProfesional(document, bf, tituloPortada, subtituloPortada, separadorPrincipal, bundle, colorPrimario, colorSecundario);

            // ========================================
            // PÁGINA 2: ÍNDICE AUTOMÁTICO
            // ========================================
            document.newPage();
            crearIndiceAutomatico(document, bf, tituloSeccion, contenidoNormal, separadorSecundario, bundle);

            // ========================================
            // PÁGINA 3: RESUMEN EJECUTIVO
            // ========================================
            document.newPage();
            crearResumenEjecutivo(document, bf, tituloSeccion, subtituloSeccion, contenidoNormal,
                                separadorSecundario, bundle, cadenaEntrada, estadoSimulacion, historialPasos, colorExito, colorError, colorPrimario);

            // ========================================
            // CONTENIDO DETALLADO
            // ========================================
            document.newPage();
            crearContenidoDetallado(document, bf, bfMono, tituloSeccion, subtituloSeccion, contenidoNormal,
                                  contenidoMono, separadorSecundario, bundle, gramaticaOriginal,
                                  tablaPredictiva, funcionesError, cadenaEntrada, estadoSimulacion,
                                  historialPasos, colorPrimario, colorSecundario, colorAcento,
                                  colorExito, colorError, colorFondoCabecera);

            // ========================================
            // CONCLUSIÓN AUTOMÁTICA
            // ========================================
            document.newPage();
            crearConclusionAutomatica(document, bf, tituloSeccion, contenidoNormal,
                                    separadorPrincipal, bundle, estadoSimulacion, colorExito, colorError);

            document.close();

        } catch (BadElementException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error de elemento en PDF", ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error de E/S en PDF", ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error general en generación de PDF", ex);
        }
        return true;
    }

    /**
     * Crea la portada profesional del informe
     */
    private void crearPortadaProfesional(Document document, BaseFont bf, Font tituloPortada, Font subtituloPortada,
                                       LineSeparator separador, ResourceBundle bundle, BaseColor colorPrimario, BaseColor colorSecundario)
                                       throws DocumentException, IOException {

        // Logo de la aplicación
        try {
            Image logo = Image.getInstance(Objects.requireNonNull(getClass().getResource("/resources/logo2Antes.png")).toExternalForm());
            logo.setAlignment(Image.ALIGN_CENTER);
            logo.scalePercent(40);
            document.add(logo);
        } catch (Exception e) {
            // Si no hay logo, continuar sin él
        }

        // Espacios
        document.add(new Paragraph(" ", new Font(bf, 30)));
        document.add(new Paragraph(" ", new Font(bf, 20)));

        // Título principal grande
        Paragraph tituloPrincipal = new Paragraph(bundle.getString("informe.profesional.portada.titulo"), tituloPortada);
        tituloPrincipal.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(tituloPrincipal);

        document.add(new Paragraph(" ", new Font(bf, 15)));

        // Subtítulo con nombre de la gramática
        Font subtituloGramatica = new Font(bf, 22, Font.BOLD);
        subtituloGramatica.setColor(colorSecundario);
        Paragraph subtitulo = new Paragraph(this.getNombre(), subtituloGramatica);
        subtitulo.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(subtitulo);

        document.add(new Paragraph(" ", new Font(bf, 25)));
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 20)));

        // Información adicional
        Font infoFont = new Font(bf, 14);
        infoFont.setColor(colorPrimario);

        Paragraph appInfo = new Paragraph(bundle.getString("informe.profesional.portada.app.nombre"), infoFont);
        appInfo.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(appInfo);

        document.add(new Paragraph(" ", new Font(bf, 10)));

        // Fecha de generación
        Font fechaFont = new Font(bf, 12);
        fechaFont.setColor(BaseColor.GRAY);
        Paragraph fecha = new Paragraph(bundle.getString("informe.profesional.generado") + " " +
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), fechaFont);
        fecha.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(fecha);
    }

    /**
     * Crea el índice automático del informe
     */
    private void crearIndiceAutomatico(Document document, BaseFont bf, Font tituloSeccion, Font contenidoNormal,
                                     LineSeparator separador, ResourceBundle bundle) throws DocumentException {

        Paragraph tituloIndice = new Paragraph(bundle.getString("informe.profesional.indice.titulo"), tituloSeccion);
        tituloIndice.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(tituloIndice);
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 15)));

        // Estructura del índice
        String[] secciones = {
            bundle.getString("informe.profesional.indice.resumen"),
            bundle.getString("informe.profesional.indice.gramatica.original"),
            bundle.getString("informe.profesional.indice.gramatica.modificada"),
            bundle.getString("informe.profesional.indice.funciones.error"),
            bundle.getString("informe.profesional.indice.tabla.predictiva"),
            bundle.getString("informe.profesional.indice.simulacion"),
            bundle.getString("informe.profesional.indice.historial"),
            bundle.getString("informe.profesional.indice.derivacion"),
            bundle.getString("informe.profesional.indice.arbol"),
            bundle.getString("informe.profesional.indice.conclusion")
        };

        for (int i = 0; i < secciones.length; i++) {
            String entrada = String.format("%d. %s", i + 1, secciones[i]);
            Paragraph itemIndice = new Paragraph(entrada, contenidoNormal);
            itemIndice.setIndentationLeft(50);
            document.add(itemIndice);
            document.add(new Paragraph(" ", new Font(bf, 5)));
        }
    }

    /**
     * Crea el índice automático específico para el informe del simulador
     */
    private void crearIndiceSimulador(Document document, BaseFont bf, Font tituloSeccion, Font contenidoNormal,
                                    LineSeparator separador, ResourceBundle bundle, boolean hasFuncionesError, boolean hasTablaPredictiva) throws DocumentException {

        Paragraph tituloIndice = new Paragraph(bundle.getString("informe.profesional.indice.titulo"), tituloSeccion);
        tituloIndice.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(tituloIndice);
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 15)));

        int seccionNumero = 1;

        // Sección 1: Gramática Original (siempre presente)
        String entrada1 = String.format("%d. %s", seccionNumero++, bundle.getString("informe.profesional.indice.gramatica.original"));
        Paragraph itemIndice1 = new Paragraph(entrada1, contenidoNormal);
        itemIndice1.setIndentationLeft(50);
        document.add(itemIndice1);
        document.add(new Paragraph(" ", new Font(bf, 5)));

        // Sección 2: Gramática Modificada (siempre presente)
        String entrada2 = String.format("%d. %s", seccionNumero++, bundle.getString("informe.profesional.indice.gramatica.modificada"));
        Paragraph itemIndice2 = new Paragraph(entrada2, contenidoNormal);
        itemIndice2.setIndentationLeft(50);
        document.add(itemIndice2);
        document.add(new Paragraph(" ", new Font(bf, 5)));

        // Sección 3: Funciones de Error (solo si existen)
        if (hasFuncionesError) {
            String entrada3 = String.format("%d. %s", seccionNumero++, bundle.getString("informe.profesional.indice.funciones.error"));
            Paragraph itemIndice3 = new Paragraph(entrada3, contenidoNormal);
            itemIndice3.setIndentationLeft(50);
            document.add(itemIndice3);
            document.add(new Paragraph(" ", new Font(bf, 5)));
        }

        // Sección 4: Tabla Predictiva (última sección si existe)
        if (hasTablaPredictiva) {
            String entrada4 = String.format("%d. %s", seccionNumero++, bundle.getString("informe.profesional.indice.tabla.predictiva"));
            Paragraph itemIndice4 = new Paragraph(entrada4, contenidoNormal);
            itemIndice4.setIndentationLeft(50);
            document.add(itemIndice4);
            document.add(new Paragraph(" ", new Font(bf, 5)));
        }
    }

    /**
     * Crea el resumen ejecutivo del informe
     */
    private void crearResumenEjecutivo(Document document, BaseFont bf, Font tituloSeccion, Font subtituloSeccion,
                                     Font contenidoNormal, LineSeparator separador, ResourceBundle bundle,
                                     String cadenaEntrada, String estadoSimulacion, List<HistorialPaso> historialPasos,
                                     BaseColor colorExito, BaseColor colorError, BaseColor colorPrimario)
                                     throws DocumentException {

        Paragraph tituloResumen = new Paragraph(bundle.getString("informe.profesional.seccion.resumen.ejecutivo"), tituloSeccion);
        tituloResumen.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(tituloResumen);
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 15)));

        // Información clave
        Font labelFont = new Font(bf, 12, Font.BOLD);
        labelFont.setColor(colorPrimario);

        // Cadena de entrada
        Paragraph labelCadena = new Paragraph(bundle.getString("informe.profesional.cadena.entrada"), labelFont);
        document.add(labelCadena);

        Font valorFont = new Font(bf, 12);
        Paragraph valorCadena = new Paragraph("    " + (cadenaEntrada != null ? cadenaEntrada : bundle.getString("informe.profesional.no.especificada")), valorFont);
        valorCadena.setIndentationLeft(20);
        document.add(valorCadena);
        document.add(new Paragraph(" ", new Font(bf, 10)));

        // Resultado final con recuadro destacado
        Paragraph labelResultado = new Paragraph(bundle.getString("informe.profesional.resultado.final"), labelFont);
        document.add(labelResultado);
        document.add(new Paragraph(" ", new Font(bf, 5)));

        BaseColor colorEstado = estadoSimulacion != null && estadoSimulacion.equals(bundle.getString("informe.simulador.estado.aceptada")) ?
                               colorExito : colorError;
        String textoEstado = estadoSimulacion != null ? estadoSimulacion : bundle.getString("informe.profesional.no.especificado");

        // Crear tabla para resaltar el resultado
        PdfPTable tablaResultado = new PdfPTable(1);
        tablaResultado.setWidthPercentage(80);
        tablaResultado.setSpacingBefore(5);
        tablaResultado.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);

        Font estadoFont = new Font(bf, 16, Font.BOLD);
        estadoFont.setColor(colorEstado);

        PdfPCell cellResultado = new PdfPCell(new Phrase(textoEstado, estadoFont));
        cellResultado.setBackgroundColor(new BaseColor(250, 250, 250)); // Fondo gris muy claro
        cellResultado.setBorderColor(colorEstado);
        cellResultado.setBorderWidth(2);
        cellResultado.setPadding(15);
        cellResultado.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cellResultado.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);

        tablaResultado.addCell(cellResultado);
        document.add(tablaResultado);
        document.add(new Paragraph(" ", new Font(bf, 15)));

        // Número de pasos
        int numPasos = historialPasos != null ? historialPasos.size() : 0;
        Paragraph labelPasos = new Paragraph(bundle.getString("informe.profesional.numero.pasos"), labelFont);
        document.add(labelPasos);

        Paragraph valorPasos = new Paragraph("    " + numPasos + " " + bundle.getString("informe.profesional.unidad.pasos"), valorFont);
        valorPasos.setIndentationLeft(20);
        document.add(valorPasos);
    }

    /**
     * Crea el contenido detallado del informe
     */
    private void crearContenidoDetallado(Document document, BaseFont bf, BaseFont bfMono, Font tituloSeccion,
                                       Font subtituloSeccion, Font contenidoNormal, Font contenidoMono,
                                       LineSeparator separador, ResourceBundle bundle, Gramatica gramaticaOriginal,
                                       TablaPredictiva tablaPredictiva, List<FuncionError> funcionesError,
                                       String cadenaEntrada, String estadoSimulacion, List<HistorialPaso> historialPasos,
                                       BaseColor colorPrimario, BaseColor colorSecundario, BaseColor colorAcento,
                                       BaseColor colorExito, BaseColor colorError, BaseColor colorFondoCabecera)
                                       throws DocumentException {

        // ========================================
        // SECCIÓN 1: GRAMÁTICA ORIGINAL
        // ========================================
        Paragraph tituloGramaticaOriginal = new Paragraph(bundle.getString("informe.profesional.seccion.gramatica.original"), tituloSeccion);
        document.add(tituloGramaticaOriginal);
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 10)));

        // Información básica
        agregarInformacionGramatica(document, bf, subtituloSeccion, contenidoNormal, contenidoMono,
                                  bundle, gramaticaOriginal, "original", colorPrimario, colorSecundario);

        // ========================================
        // SECCIÓN 2: GRAMÁTICA MODIFICADA
        // ========================================
        document.newPage();
        Paragraph tituloGramaticaModificada = new Paragraph(bundle.getString("informe.profesional.seccion.gramatica.modificada"), tituloSeccion);
        document.add(tituloGramaticaModificada);
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 10)));

        agregarInformacionGramatica(document, bf, subtituloSeccion, contenidoNormal, contenidoMono,
                                  bundle, this, "modificada", colorPrimario, colorSecundario);

        // ========================================
        // SECCIÓN 3: FUNCIONES DE ERROR
        // ========================================
        if (funcionesError != null && !funcionesError.isEmpty()) {
            document.newPage();
            Paragraph tituloFuncionesError = new Paragraph(bundle.getString("informe.profesional.seccion.funciones.error"), tituloSeccion);
            document.add(tituloFuncionesError);
            document.add(new Chunk(separador));
            document.add(new Paragraph(" ", new Font(bf, 10)));

            agregarFuncionesError(document, bf, contenidoNormal, bundle, funcionesError,
                                colorPrimario, colorSecundario, colorFondoCabecera, colorAcento);
        }

        // ========================================
        // SECCIÓN 4: TABLA PREDICTIVA
        // ========================================
        if (tablaPredictiva != null) {
            document.newPage();
            Paragraph tituloTablaPredictiva = new Paragraph(bundle.getString("informe.profesional.seccion.tabla.predictiva"), tituloSeccion);
            document.add(tituloTablaPredictiva);
            document.add(new Chunk(separador));
            document.add(new Paragraph(" ", new Font(bf, 10)));

            agregarTablaPredictivaMejorada(document, tablaPredictiva, bundle, bf, contenidoNormal,
                                         colorPrimario, colorSecundario, colorFondoCabecera);
        }

        // ========================================
        // SECCIÓN 5: INFORMACIÓN DE SIMULACIÓN
        // ========================================
        document.newPage();
        Paragraph tituloSimulacion = new Paragraph(bundle.getString("informe.profesional.seccion.simulacion"), tituloSeccion);
        document.add(tituloSimulacion);
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 10)));

        agregarInformacionSimulacion(document, bf, subtituloSeccion, contenidoNormal, bundle,
                                   cadenaEntrada, estadoSimulacion, colorExito, colorError);

        // ========================================
        // SECCIÓN 6: HISTORIAL DE PASOS
        // ========================================
        if (historialPasos != null && !historialPasos.isEmpty()) {
            document.newPage();
            Paragraph tituloHistorial = new Paragraph(bundle.getString("informe.profesional.seccion.historial"), tituloSeccion);
            document.add(tituloHistorial);
            document.add(new Chunk(separador));
            document.add(new Paragraph(" ", new Font(bf, 10)));

            agregarHistorialMejorado(document, bf, contenidoNormal, bundle, historialPasos,
                                   colorPrimario, colorSecundario, colorFondoCabecera, colorAcento);
        }

        // ========================================
        // SECCIÓN 7: DERIVACIÓN
        // ========================================
        if (historialPasos != null && !historialPasos.isEmpty()) {
            document.newPage();
            Paragraph tituloDerivacion = new Paragraph(bundle.getString("informe.profesional.seccion.derivacion"), tituloSeccion);
            document.add(tituloDerivacion);
            document.add(new Chunk(separador));
            document.add(new Paragraph(" ", new Font(bf, 10)));

            agregarDerivacionMejorada(document, bfMono, contenidoMono, bundle, historialPasos, colorPrimario);
        }

        // ========================================
        // SECCIÓN 8: ÁRBOL SINTÁCTICO
        // ========================================
        document.newPage();
        Paragraph tituloArbol = new Paragraph(bundle.getString("informe.profesional.seccion.arbol"), tituloSeccion);
        document.add(tituloArbol);
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 10)));

        agregarArbolSintacticoMejorado(document, bf, contenidoNormal, bundle, historialPasos, colorPrimario);
    }

    /**
     * Agrega información detallada de una gramática con formato mejorado
     */
    private void agregarInformacionGramatica(Document document, BaseFont bf, Font subtituloSeccion, Font contenidoNormal,
                                           Font contenidoMono, ResourceBundle bundle, Gramatica gramatica,
                                           String tipo, BaseColor colorPrimario, BaseColor colorSecundario)
                                           throws DocumentException {

        // Descripción
        Paragraph subDesc = new Paragraph(bundle.getString("informe.profesional.descripcion"), subtituloSeccion);
        document.add(subDesc);
        Paragraph desc = new Paragraph("    " + gramatica.getDescripcion(), contenidoNormal);
        desc.setIndentationLeft(20);
        document.add(desc);
        document.add(new Paragraph(" ", new Font(bf, 8)));

        // Símbolo inicial
        Paragraph subSimbolo = new Paragraph(bundle.getString("informe.profesional.simbolo.inicial"), subtituloSeccion);
        document.add(subSimbolo);
        Font simboloFont = new Font(bf, 11);
        simboloFont.setColor(BaseColor.BLACK);
        Paragraph simbolo = new Paragraph("    " + gramatica.getSimbInicial(), simboloFont);
        simbolo.setIndentationLeft(20);
        document.add(simbolo);
        document.add(new Paragraph(" ", new Font(bf, 8)));

        // Símbolos no terminales
        Paragraph subNoTerm = new Paragraph(bundle.getString("informe.profesional.simbolos.no.terminales"), subtituloSeccion);
        document.add(subNoTerm);

        ObservableList<String> noTerm = gramatica.getNoTerminalesModel();
        for (String nt : noTerm) {
            Font ntFont = new Font(bf, 11);
            ntFont.setColor(BaseColor.BLACK);
            Paragraph ntPara = new Paragraph("    • " + nt, ntFont);
            ntPara.setIndentationLeft(20);
            document.add(ntPara);
        }
        document.add(new Paragraph(" ", new Font(bf, 8)));

        // Símbolos terminales
        Paragraph subTerm = new Paragraph(bundle.getString("informe.profesional.simbolos.terminales"), subtituloSeccion);
        document.add(subTerm);

        ObservableList<String> term = gramatica.getTerminalesModel();
        for (String t : term) {
            Font termFont = new Font(bf, 11);
            termFont.setColor(BaseColor.BLACK);
            Paragraph termPara = new Paragraph("    • " + t, termFont);
            termPara.setIndentationLeft(20);
            document.add(termPara);
        }
        document.add(new Paragraph(" ", new Font(bf, 8)));

        // Producciones
        Paragraph subProd = new Paragraph(bundle.getString("informe.profesional.producciones"), subtituloSeccion);
        document.add(subProd);
        document.add(new Paragraph(" ", new Font(bf, 5)));

        ObservableList<String> prod = gramatica.getProduccionesModel();
        int index = 1;
        for (String produccion : prod) {
            // Crear tabla para producción con formato simplificado
            PdfPTable tablaProd = new PdfPTable(2);
            tablaProd.setWidthPercentage(95);
            tablaProd.setWidths(new float[]{0.15f, 0.85f}); // Proporciones: número, producción completa

            Font numeroFont = new Font(bf, 10, Font.BOLD);
            numeroFont.setColor(colorPrimario);
            Font prodFont = new Font(contenidoMono);
            prodFont.setColor(BaseColor.BLACK);

            // Convertir la flecha Unicode a ASCII solo para el PDF
            String produccionFormateada = produccion.replace("→", " -> ");

            // Celda del número
            PdfPCell cellNumero = new PdfPCell(new Phrase(String.valueOf(index), numeroFont));
            cellNumero.setBackgroundColor(new BaseColor(240, 248, 255)); // Azul muy claro
            cellNumero.setBorderColor(colorPrimario);
            cellNumero.setBorderWidth(1);
            cellNumero.setPadding(8);
            cellNumero.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cellNumero.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            tablaProd.addCell(cellNumero);

            // Celda de la producción completa
            PdfPCell cellProd = new PdfPCell(new Phrase(produccionFormateada, prodFont));
            cellProd.setBackgroundColor(new BaseColor(248, 249, 250)); // Gris muy claro
            cellProd.setBorderColor(colorSecundario);
            cellProd.setBorderWidth(1);
            cellProd.setPadding(8);
            cellProd.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            cellProd.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
            tablaProd.addCell(cellProd);

            document.add(tablaProd);
            document.add(new Paragraph(" ", new Font(bf, 5)));
            index++;
        }
    }

    /**
     * Agrega sección de funciones de error con formato tabular mejorado
     */
    private void agregarFuncionesError(Document document, BaseFont bf, Font contenidoNormal, ResourceBundle bundle,
                                     List<FuncionError> funcionesError, BaseColor colorPrimario,
                                     BaseColor colorSecundario, BaseColor colorFondo, BaseColor colorAcento)
                                     throws DocumentException {

        PdfPTable tablaFunciones = new PdfPTable(4);
        tablaFunciones.setWidthPercentage(100);
        tablaFunciones.setSpacingBefore(5);
        tablaFunciones.setWidths(new float[]{1, 2, 2, 4});

        // Fuentes
        Font headerFont = new Font(bf, 9, Font.BOLD);
        headerFont.setColor(BaseColor.WHITE);
        Font dataFont = new Font(bf, 8);

        // Encabezados
        String[] headers = {"ID", "Acción", "Símbolo", "Descripción"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(colorPrimario);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setPadding(8);
            cell.setBorderColor(colorSecundario);
            cell.setBorderWidth(1);
            tablaFunciones.addCell(cell);
        }

        // Datos
        boolean filaAlterna = false;
        for (FuncionError funcion : funcionesError) {
            BaseColor colorFondoFila = filaAlterna ? colorFondo : BaseColor.WHITE;

            // ID
            PdfPCell cellId = new PdfPCell(new Phrase(String.valueOf(funcion.getIdentificador()), dataFont));
            cellId.setBackgroundColor(colorFondoFila);
            cellId.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cellId.setPadding(6);
            cellId.setBorderColor(colorSecundario);
            cellId.setBorderWidth(0.5f);
            tablaFunciones.addCell(cellId);

            // Acción
            String nombreAccion = bundle.getString(funcion.getNombreAccion());
            Font accionFont = new Font(bf, 8);
            accionFont.setColor(colorAcento);
            PdfPCell cellAccion = new PdfPCell(new Phrase(nombreAccion, accionFont));
            cellAccion.setBackgroundColor(colorFondoFila);
            cellAccion.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            cellAccion.setPadding(6);
            cellAccion.setBorderColor(colorSecundario);
            cellAccion.setBorderWidth(0.5f);
            tablaFunciones.addCell(cellAccion);

            // Símbolo
            String simbolo = funcion.getSimbolo() != null ? funcion.getSimbolo().getNombre() : "-";
            Font simboloFont = new Font(bf, 8, Font.BOLD);
            simboloFont.setColor(funcion.getSimbolo() != null ? BaseColor.BLACK : BaseColor.GRAY);
            PdfPCell cellSimbolo = new PdfPCell(new Phrase(simbolo, simboloFont));
            cellSimbolo.setBackgroundColor(colorFondoFila);
            cellSimbolo.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cellSimbolo.setPadding(6);
            cellSimbolo.setBorderColor(colorSecundario);
            cellSimbolo.setBorderWidth(0.5f);
            tablaFunciones.addCell(cellSimbolo);

            // Descripción completa
            String descripcion = getDescripcionFuncionError(funcion, bundle);
            if (funcion.getMensaje() != null && !funcion.getMensaje().isEmpty()) {
                descripcion += " (" + funcion.getMensaje() + ")";
            }
            PdfPCell cellDescripcion = new PdfPCell(new Phrase(descripcion, dataFont));
            cellDescripcion.setBackgroundColor(colorFondoFila);
            cellDescripcion.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            cellDescripcion.setPadding(6);
            cellDescripcion.setBorderColor(colorSecundario);
            cellDescripcion.setBorderWidth(0.5f);
            tablaFunciones.addCell(cellDescripcion);

            filaAlterna = !filaAlterna;
        }

        document.add(tablaFunciones);
    }

    /**
     * Agrega tabla predictiva con mejor formato visual
     */
    private void agregarTablaPredictivaMejorada(Document document, TablaPredictiva tablaPredictiva,
                                              ResourceBundle bundle, BaseFont bf, Font contenido,
                                              BaseColor colorPrimario, BaseColor colorSecundario, BaseColor colorFondo)
                                              throws DocumentException {

        List<FilaTablaPredictiva> filas = tablaPredictiva.getFilas();
        if (filas == null || filas.isEmpty()) return;

        List<Terminal> terminales = this.getTerminales();
        if (terminales.isEmpty()) return;

        int numColumnas = terminales.size() + 1;
        PdfPTable tabla = new PdfPTable(numColumnas);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(5);

        // Fuentes para la tabla
        Font headerFont = new Font(bf, 9, Font.BOLD);
        headerFont.setColor(BaseColor.WHITE);

        // Encabezado de símbolo
        PdfPCell cellSimbolo = new PdfPCell(new Phrase(bundle.getString("informe.profesional.tabla.simbolo"), headerFont));
        cellSimbolo.setBackgroundColor(colorPrimario);
        cellSimbolo.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cellSimbolo.setPadding(8);
        cellSimbolo.setBorderColor(colorSecundario);
        cellSimbolo.setBorderWidth(1);
        tabla.addCell(cellSimbolo);

        // Encabezados de terminales
        for (Terminal terminal : terminales) {
            PdfPCell cellTerm = new PdfPCell(new Phrase(terminal.getNombre(), headerFont));
            cellTerm.setBackgroundColor(colorPrimario);
            cellTerm.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cellTerm.setPadding(8);
            cellTerm.setBorderColor(colorSecundario);
            cellTerm.setBorderWidth(1);
            tabla.addCell(cellTerm);
        }

        // Filas de datos
        boolean filaAlterna = false;
        for (FilaTablaPredictiva fila : filas) {
            BaseColor colorFondoFila = filaAlterna ? colorFondo : BaseColor.WHITE;

            // Celda del símbolo
            String simbolo = fila.getSimbolo();
            Font simboloFont = new Font(bf, 8, fila.getEsTerminal() ? Font.NORMAL : Font.BOLD);
            simboloFont.setColor(fila.getEsTerminal() ? BaseColor.BLACK : colorPrimario);

            PdfPCell cellSim = new PdfPCell(new Phrase(simbolo, simboloFont));
            cellSim.setBackgroundColor(colorFondoFila);
            cellSim.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cellSim.setPadding(6);
            cellSim.setBorderColor(colorSecundario);
            cellSim.setBorderWidth(0.5f);
            tabla.addCell(cellSim);

            // Celdas de valores
            for (Terminal terminal : terminales) {
                String valor = fila.getValor(terminal.getNombre()).get();
                String textoCelda = (valor != null && !valor.isEmpty()) ? valor : "";

                Font valorFont = new Font(bf, 8);
                BaseColor colorFondoValor = colorFondoFila;

                // Colores específicos según tipo de contenido
                if (textoCelda.contains("→")) {
                    valorFont.setColor(new BaseColor(0, 100, 0)); // Verde para producciones
                    colorFondoValor = new BaseColor(240, 255, 240); // Verde muy claro
                } else if (textoCelda.startsWith("ε")) {
                    valorFont.setColor(new BaseColor(255, 140, 0)); // Naranja para épsilon
                    colorFondoValor = new BaseColor(255, 248, 240); // Naranja muy claro
                } else if (textoCelda.matches("\\d+")) {
                    valorFont.setColor(new BaseColor(100, 100, 255)); // Azul para funciones error
                    colorFondoValor = new BaseColor(240, 240, 255); // Azul muy claro
                }

                PdfPCell cellValor = new PdfPCell(new Phrase(textoCelda, valorFont));
                cellValor.setBackgroundColor(colorFondoValor);
                cellValor.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                cellValor.setPadding(6);
                cellValor.setBorderColor(colorSecundario);
                cellValor.setBorderWidth(0.5f);
                tabla.addCell(cellValor);
            }

            filaAlterna = !filaAlterna;
        }

        document.add(tabla);
    }

    /**
     * Agrega información de simulación mejorada
     */
    private void agregarInformacionSimulacion(Document document, BaseFont bf, Font subtituloSeccion, Font contenidoNormal,
                                            ResourceBundle bundle, String cadenaEntrada, String estadoSimulacion,
                                            BaseColor colorExito, BaseColor colorError) throws DocumentException {

        // Cadena de entrada
        Paragraph subCadena = new Paragraph(bundle.getString("informe.profesional.cadena.entrada"), subtituloSeccion);
        document.add(subCadena);

        Font cadenaFont = new Font(bf, 12, Font.BOLD);
        cadenaFont.setColor(BaseColor.BLACK);
        Paragraph cadena = new Paragraph("    " + (cadenaEntrada != null ? cadenaEntrada : bundle.getString("informe.profesional.no.especificada")), cadenaFont);
        cadena.setIndentationLeft(20);
        document.add(cadena);
        document.add(new Paragraph(" ", new Font(bf, 10)));

        // Estado final destacado
        Paragraph subEstado = new Paragraph(bundle.getString("informe.profesional.resultado.final"), subtituloSeccion);
        document.add(subEstado);

        BaseColor colorEstado = estadoSimulacion != null && estadoSimulacion.equals(bundle.getString("informe.simulador.estado.aceptada")) ?
                               colorExito : colorError;
        Font estadoFont = new Font(bf, 16, Font.BOLD);
        estadoFont.setColor(colorEstado);

        Paragraph estado = new Paragraph("    " + (estadoSimulacion != null ? estadoSimulacion : bundle.getString("informe.profesional.no.especificado")), estadoFont);
        estado.setAlignment(Paragraph.ALIGN_CENTER);
        estado.setIndentationLeft(20);
        document.add(estado);
    }

    /**
     * Agrega historial de pasos mejorado con colores
     */
    private void agregarHistorialMejorado(Document document, BaseFont bf, Font contenidoNormal, ResourceBundle bundle,
                                        List<HistorialPaso> historialPasos, BaseColor colorPrimario,
                                        BaseColor colorSecundario, BaseColor colorFondo, BaseColor colorAcento)
                                        throws DocumentException {

        PdfPTable tablaHistorial = new PdfPTable(4);
        tablaHistorial.setWidthPercentage(100);
        tablaHistorial.setSpacingBefore(5);
        tablaHistorial.setWidths(new float[]{1, 2, 2, 3});

        // Fuentes
        Font headerFont = new Font(bf, 9, Font.BOLD);
        headerFont.setColor(BaseColor.WHITE);
        Font dataFont = new Font(bf, 8);

        // Encabezados usando las claves correctas del bundle
        String[] headerKeys = {"paso", "pila", "entrada", "accion"};
        for (String key : headerKeys) {
            PdfPCell cell = new PdfPCell(new Phrase(bundle.getString("informe.simulador.historial." + key), headerFont));
            cell.setBackgroundColor(colorPrimario);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setPadding(8);
            cell.setBorderColor(colorSecundario);
            cell.setBorderWidth(1);
            tablaHistorial.addCell(cell);
        }

        // Datos con colores diferenciados
        boolean filaAlterna = false;
        for (HistorialPaso paso : historialPasos) {
            BaseColor colorFondoFila = filaAlterna ? colorFondo : BaseColor.WHITE;

            // Paso
            PdfPCell cellPaso = new PdfPCell(new Phrase(paso.getPaso(), dataFont));
            cellPaso.setBackgroundColor(colorFondoFila);
            cellPaso.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cellPaso.setPadding(6);
            cellPaso.setBorderColor(colorSecundario);
            cellPaso.setBorderWidth(0.5f);
            tablaHistorial.addCell(cellPaso);

            // Pila
            PdfPCell cellPila = new PdfPCell(new Phrase(paso.getPila(), dataFont));
            cellPila.setBackgroundColor(colorFondoFila);
            cellPila.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            cellPila.setPadding(6);
            cellPila.setBorderColor(colorSecundario);
            cellPila.setBorderWidth(0.5f);
            tablaHistorial.addCell(cellPila);

            // Entrada
            PdfPCell cellEntrada = new PdfPCell(new Phrase(paso.getEntrada(), dataFont));
            cellEntrada.setBackgroundColor(colorFondoFila);
            cellEntrada.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            cellEntrada.setPadding(6);
            cellEntrada.setBorderColor(colorSecundario);
            cellEntrada.setBorderWidth(0.5f);
            tablaHistorial.addCell(cellEntrada);

            // Acción con colores diferenciados
            String accionOriginal = paso.getAccion();
            // Convertir flechas Unicode a ASCII para mostrar en el PDF
            String accion = accionOriginal.replace("→", " -> ");
            Font accionFont = new Font(bf, 8);
            BaseColor colorFondoAccion = colorFondoFila;

            if (accion.equals("Emparejar")) {
                accionFont.setColor(new BaseColor(52, 152, 219)); // Azul para emparejar
                colorFondoAccion = new BaseColor(240, 248, 255);
            } else if (accion.contains(" -> ")) {
                accionFont.setColor(new BaseColor(46, 204, 113)); // Verde para derivaciones
                colorFondoAccion = new BaseColor(240, 255, 240);
            } else if (accion.equals("Error")) {
                accionFont.setColor(new BaseColor(231, 76, 60)); // Rojo para errores
                colorFondoAccion = new BaseColor(255, 240, 240);
            } else if (accion.equals("Aceptar")) {
                accionFont.setColor(new BaseColor(230, 126, 34)); // Naranja para aceptar
                accionFont.setStyle(Font.BOLD);
                colorFondoAccion = new BaseColor(255, 248, 240);
            }

            PdfPCell cellAccion = new PdfPCell(new Phrase(accion, accionFont));
            cellAccion.setBackgroundColor(colorFondoAccion);
            cellAccion.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
            cellAccion.setPadding(6);
            cellAccion.setBorderColor(colorSecundario);
            cellAccion.setBorderWidth(0.5f);
            tablaHistorial.addCell(cellAccion);

            filaAlterna = !filaAlterna;
        }

        document.add(tablaHistorial);
    }

    /**
     * Agrega derivación con formato de código
     */
    private void agregarDerivacionMejorada(Document document, BaseFont bfMono, Font contenidoMono,
                                         ResourceBundle bundle, List<HistorialPaso> historialPasos,
                                         BaseColor colorPrimario) throws DocumentException {

        Font tituloFont = new Font(bfMono, 11, Font.BOLD);
        tituloFont.setColor(colorPrimario);

        for (int i = 0; i < historialPasos.size(); i++) {
            HistorialPaso paso = historialPasos.get(i);
            // Convertir flechas Unicode a ASCII para mostrar en el PDF
            String accionFormateada = paso.getAccion().replace("→", " -> ");
            String derivacionLine = bundle.getString("informe.profesional.derivacion.paso") + " " + (i + 1) + ": " + accionFormateada;

            // Crear bloque para cada paso
            PdfPTable tablaPaso = new PdfPTable(1);
            tablaPaso.setWidthPercentage(95);

            PdfPCell cellPaso = new PdfPCell(new Phrase(derivacionLine, contenidoMono));
            cellPaso.setBackgroundColor(new BaseColor(248, 249, 250));
            cellPaso.setPadding(8);
            cellPaso.setBorderColor(colorPrimario);
            cellPaso.setBorderWidth(1);

            tablaPaso.addCell(cellPaso);
            document.add(tablaPaso);
            document.add(new Paragraph(" ", new Font(bfMono, 3)));
        }
    }

    /**
     * Agrega árbol sintáctico con colores diferenciados
     */
    private void agregarArbolSintacticoMejorado(Document document, BaseFont bf, Font contenidoNormal,
                                               ResourceBundle bundle, List<HistorialPaso> historialPasos,
                                               BaseColor colorPrimario) throws DocumentException {

        try {
            agregarImagenArbolAlPDF(document, historialPasos);
        } catch (Exception e) {
            // Si no se puede generar la imagen, mostrar información textual con colores
            Paragraph notaArbol = new Paragraph("Árbol Sintáctico (Vista Textual):", contenidoNormal);
            document.add(notaArbol);
            document.add(new Paragraph(" ", new Font(bf, 8)));

            // Crear representación textual coloreada
            if (historialPasos != null && !historialPasos.isEmpty()) {
                NodoArbol raiz = construirArbolDesdeHistorial(historialPasos);
                agregarRepresentacionArbolTextual(document, bf, raiz, "", colorPrimario);
            }
        }
    }

    /**
     * Agrega representación textual del árbol con colores
     */
    private void agregarRepresentacionArbolTextual(Document document, BaseFont bf, NodoArbol nodo,
                                                 String prefijo, BaseColor colorPrimario) throws DocumentException {

        Font nodoFont = new Font(bf, 10);
        if (esTerminal(nodo.valor)) {
            nodoFont.setColor(BaseColor.BLACK); // Terminales en negro
        } else {
            nodoFont.setColor(colorPrimario); // No terminales en azul
        }

        String simboloEspecial = "";
        if (nodo.valor.equals("$") || nodo.valor.equals("ε") || nodo.valor.equals(";")) {
            nodoFont.setColor(BaseColor.GRAY); // Símbolos especiales en gris
            simboloEspecial = " (" + nodo.valor + ")";
        }

        Paragraph nodoPara = new Paragraph(prefijo + "├── " + nodo.valor + simboloEspecial, nodoFont);
        nodoPara.setIndentationLeft(20 + prefijo.length() * 10);
        document.add(nodoPara);

        // Procesar hijos recursivamente
        for (int i = 0; i < nodo.hijos.size(); i++) {
            NodoArbol hijo = nodo.hijos.get(i);
            String nuevoPrefijo = prefijo + (i < nodo.hijos.size() - 1 ? "│   " : "    ");
            agregarRepresentacionArbolTextual(document, bf, hijo, nuevoPrefijo, colorPrimario);
        }
    }

    /**
     * Crea la conclusión automática del informe
     */
    private void crearConclusionAutomatica(Document document, BaseFont bf, Font tituloSeccion, Font contenidoNormal,
                                         LineSeparator separador, ResourceBundle bundle, String estadoSimulacion,
                                         BaseColor colorExito, BaseColor colorError) throws DocumentException {

        Paragraph tituloConclusion = new Paragraph(bundle.getString("informe.profesional.conclusion.titulo"), tituloSeccion);
        tituloConclusion.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(tituloConclusion);
        document.add(new Chunk(separador));
        document.add(new Paragraph(" ", new Font(bf, 15)));

        // Conclusión automática basada en el estado
        String conclusion;
        BaseColor colorConclusion;
        Font conclusionFont = new Font(bf, 14, Font.BOLD);

        if (estadoSimulacion != null && estadoSimulacion.equals(bundle.getString("informe.simulador.estado.aceptada"))) {
            conclusion = bundle.getString("informe.profesional.conclusion.aceptada");
            colorConclusion = colorExito;
        } else if (estadoSimulacion != null && estadoSimulacion.equals(bundle.getString("informe.simulador.estado.rechazada"))) {
            conclusion = bundle.getString("informe.profesional.conclusion.rechazada");
            colorConclusion = colorError;
        } else {
            conclusion = bundle.getString("informe.profesional.conclusion.no.determinado");
            colorConclusion = BaseColor.GRAY;
        }

        conclusionFont.setColor(colorConclusion);

        // Crear tabla para resaltar la conclusión
        PdfPTable tablaConclusion = new PdfPTable(1);
        tablaConclusion.setWidthPercentage(85);
        tablaConclusion.setSpacingBefore(5);
        tablaConclusion.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);

        PdfPCell cellConclusion = new PdfPCell(new Phrase(conclusion, conclusionFont));
        cellConclusion.setBackgroundColor(new BaseColor(250, 250, 250)); // Fondo gris muy claro
        cellConclusion.setBorderColor(colorConclusion);
        cellConclusion.setBorderWidth(2);
        cellConclusion.setPadding(20);
        cellConclusion.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cellConclusion.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);

        tablaConclusion.addCell(cellConclusion);
        document.add(tablaConclusion);

        document.add(new Paragraph(" ", new Font(bf, 25)));

        // Información adicional
        Font infoFont = new Font(bf, 10);
        infoFont.setColor(BaseColor.GRAY);
        Paragraph infoAdicional = new Paragraph(bundle.getString("informe.profesional.conclusion.creditos"), infoFont);
        infoAdicional.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(infoAdicional);

        Paragraph fechaConclusion = new Paragraph(bundle.getString("informe.profesional.fecha") + " " +
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), infoFont);
        fechaConclusion.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(fechaConclusion);
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
        // Implementación correcta para recursividad por la izquierda (directa e indirecta)
        // 1) Reescritura para eliminar recursividad indirecta
        // 2) Eliminación de recursividad directa por no terminal

        // Preparar estructuras de trabajo
        ObservableList<String> noTermModel = FXCollections.observableArrayList(getNoTerminalesModel());
        ObservableList<String> prodsModel = getProduccionesModel();

        // Mapa: NT -> lista de alternativas (cada alternativa es lista de símbolos; ε = lista vacía)
        Map<String, List<List<String>>> produccionesPorNT = new LinkedHashMap<>();
        for (String nt : noTermModel) {
            produccionesPorNT.put(nt, new ArrayList<>());
        }

        for (String prodStr : prodsModel) {
            String[] partes = prodStr.split(" → ");
            if (partes.length != 2) continue;
            String A = partes[0].trim();
            String rhs = partes[1].trim();
            List<String> alt;
            if (rhs.equals("ε") || rhs.isEmpty()) {
                alt = new ArrayList<>(); // ε como lista vacía
            } else {
                alt = new ArrayList<>(Arrays.asList(rhs.split("\\s+")));
            }
            produccionesPorNT.computeIfAbsent(A, k -> new ArrayList<>()).add(alt);
        }

        boolean huboCambios = false;

        // Eliminar recursividad indirecta y luego directa para cada no terminal en orden
        for (int i = 0; i < noTermModel.size(); i++) {
            String Ai = noTermModel.get(i);
            List<List<String>> AiAlternativas = produccionesPorNT.getOrDefault(Ai, new ArrayList<>());

            // Sustitución para recursividad indirecta: para todo Aj con j < i
            for (int j = 0; j < i; j++) {
                String Aj = noTermModel.get(j);
                List<List<String>> AjAlternativas = produccionesPorNT.getOrDefault(Aj, new ArrayList<>());
                if (AjAlternativas.isEmpty()) continue;

                List<List<String>> nuevasAlternativasAi = new ArrayList<>();
                boolean reemplazo = false;
                for (List<String> alt : AiAlternativas) {
                    if (!alt.isEmpty() && alt.get(0).equals(Aj)) {
                        // Ai -> Aj γ  ==>  Ai -> δ γ  para cada Aj -> δ
                        List<String> gamma = alt.subList(1, alt.size());
                        for (List<String> delta : AjAlternativas) {
                            List<String> combinada = new ArrayList<>();
                            combinada.addAll(delta); // δ (vacía significa ε)
                            combinada.addAll(gamma); // + γ
                            nuevasAlternativasAi.add(combinada);
                        }
                        reemplazo = true;
                    } else {
                        nuevasAlternativasAi.add(new ArrayList<>(alt));
                    }
                }

                if (reemplazo) {
                    // Deduplicar preservando orden
                    LinkedHashSet<String> visto = new LinkedHashSet<>();
                    List<List<String>> dedup = new ArrayList<>();
                    for (List<String> alt : nuevasAlternativasAi) {
                        String clave = String.join(" ", alt);
                        if (visto.add(clave)) dedup.add(alt);
                    }
                    produccionesPorNT.put(Ai, dedup);
                    AiAlternativas = dedup;
                    huboCambios = true;
                }
            }

            // Eliminar recursividad directa Ai -> Ai α
            List<List<String>> alphas = new ArrayList<>();
            List<List<String>> betas = new ArrayList<>();
            for (List<String> alt : AiAlternativas) {
                if (!alt.isEmpty() && alt.get(0).equals(Ai)) {
                    alphas.add(new ArrayList<>(alt.subList(1, alt.size()))); // α
                } else {
                    betas.add(new ArrayList<>(alt)); // β (puede ser ε como lista vacía)
                }
            }

            if (!alphas.isEmpty()) {
                // Crear nuevo no terminal Ai'
                String base = Ai + "'";
                Set<String> existentes = new HashSet<>(noTermModel);
                existentes.addAll(produccionesPorNT.keySet());
                String AiPrima = generarNombreNoTerminalUnico(base, existentes);

                if (!produccionesPorNT.containsKey(AiPrima)) {
                    produccionesPorNT.put(AiPrima, new ArrayList<>());
                }
                if (!noTermModel.contains(AiPrima)) {
                    noTermModel.add(AiPrima);
                }

                // Ai -> β Ai'  (si β es ε, entonces Ai -> Ai')
                List<List<String>> nuevasAi = new ArrayList<>();
                for (List<String> beta : betas) {
                    List<String> nueva = new ArrayList<>(beta);
                    nueva.add(AiPrima);
                    nuevasAi.add(nueva);
                }
                if (nuevasAi.isEmpty()) {
                    // Caso extremo: no había β, generamos Ai -> Ai'
                    nuevasAi.add(new ArrayList<>(Collections.singletonList(AiPrima)));
                }

                // Ai' -> α Ai' | ε
                List<List<String>> nuevasAiPrima = produccionesPorNT.get(AiPrima);
                for (List<String> alpha : alphas) {
                    List<String> nueva = new ArrayList<>(alpha);
                    nueva.add(AiPrima);
                    nuevasAiPrima.add(nueva);
                }
                nuevasAiPrima.add(new ArrayList<>()); // ε

                produccionesPorNT.put(Ai, nuevasAi);
                huboCambios = true;
            }
        }

        if (huboCambios) {
            // Reconstruir modelos
            ObservableList<String> nuevoNoTerm = FXCollections.observableArrayList(noTermModel);
            ObservableList<String> nuevasProducciones = FXCollections.observableArrayList();
            for (String nt : noTermModel) {
                List<List<String>> alts = produccionesPorNT.getOrDefault(nt, Collections.emptyList());
                for (List<String> alt : alts) {
                    String rhs = alt.isEmpty() ? "ε" : String.join(" ", alt);
                    nuevasProducciones.add(nt + " → " + rhs);
                }
            }
            this.setNoTerminalesModel(nuevoNoTerm);
            this.setProduccionesModel(nuevasProducciones);
        }

        return huboCambios;
    }


    /**
     * Verifica si la gramática requiere factorización y la realiza si es necesario.
     *
     * @return true si la gramática fue factorizada, false si ya estaba correcta.
     */
    public boolean factorizar() {
        // Factorización por prefijos comunes (longest common prefix), iterativa y por no terminal
        ObservableList<String> noTermModel = FXCollections.observableArrayList(getNoTerminalesModel());
        ObservableList<String> prodsModel = getProduccionesModel();

        // Mapa: NT -> alternativas (ε = lista vacía)
        Map<String, List<List<String>>> produccionesPorNT = new LinkedHashMap<>();
        for (String nt : noTermModel) {
            produccionesPorNT.put(nt, new ArrayList<>());
        }
        for (String prodStr : prodsModel) {
            String[] partes = prodStr.split(" → ");
            if (partes.length != 2) continue;
            String A = partes[0].trim();
            String rhs = partes[1].trim();
            List<String> alt = rhs.equals("ε") || rhs.isEmpty()
                    ? new ArrayList<>()
                    : new ArrayList<>(Arrays.asList(rhs.split("\\s+")));
            produccionesPorNT.computeIfAbsent(A, k -> new ArrayList<>()).add(alt);
        }

        boolean huboCambios = false;

        for (int idx = 0; idx < noTermModel.size(); idx++) {
            String A = noTermModel.get(idx);
            List<List<String>> alternativas = produccionesPorNT.getOrDefault(A, new ArrayList<>());

            boolean factorizadoEnEsteNT;
            do {
                factorizadoEnEsteNT = false;

                // Agrupar por primer símbolo para candidatos
                Map<String, List<List<String>>> grupos = new LinkedHashMap<>();
                for (List<String> alt : alternativas) {
                    String clave = alt.isEmpty() ? "" : alt.get(0);
                    grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(alt);
                }

                int mejorL = 0;
                String mejorClave = null;
                List<List<String>> mejorGrupo = null;

                for (Map.Entry<String, List<List<String>>> e : grupos.entrySet()) {
                    List<List<String>> grupo = e.getValue();
                    if (grupo.size() < 2) continue;

                    // Calcular LCP (longest common prefix) entre todas las alternativas del grupo
                    int lcp = grupo.get(0).size();
                    for (int i = 1; i < grupo.size(); i++) {
                        lcp = Math.min(lcp, longitudPrefijoComun(grupo.get(0), grupo.get(i)));
                        if (lcp == 0) break;
                    }
                    if (lcp > mejorL) {
                        mejorL = lcp;
                        mejorClave = e.getKey();
                        mejorGrupo = grupo;
                    }
                }

                if (mejorL > 0 && mejorGrupo != null) {
                    // Prefijo común de longitud mejorL para factorización
                    List<String> prefijo = new ArrayList<>(mejorGrupo.get(0).subList(0, mejorL));

                    // Crear nuevo no terminal A'
                    String base = A + "'";
                    Set<String> existentes = new HashSet<>(noTermModel);
                    existentes.addAll(produccionesPorNT.keySet());
                    String APrima = generarNombreNoTerminalUnico(base, existentes);

                    noTermModel.add(APrima);
                    produccionesPorNT.putIfAbsent(APrima, new ArrayList<>());

                    // Quitar las alternativas del grupo de A y añadir A -> prefijo A'
                    List<List<String>> nuevasA = new ArrayList<>();
                    // Mantener las alternativas que no están en mejorGrupo
                    for (List<String> alt : alternativas) {
                        if (!mejorGrupo.contains(alt)) {
                            nuevasA.add(alt);
                        }
                    }
                    List<String> nuevaAltA = new ArrayList<>(prefijo);
                    nuevaAltA.add(APrima);
                    nuevasA.add(nuevaAltA);

                    // Añadir alternativas a A'
                    List<List<String>> alternativasAPrima = produccionesPorNT.get(APrima);
                    for (List<String> alt : mejorGrupo) {
                        List<String> sufijo = new ArrayList<>(alt.subList(mejorL, alt.size()));
                        alternativasAPrima.add(sufijo); // si sufijo vacío, será ε
                    }

                    alternativas = nuevasA;
                    produccionesPorNT.put(A, alternativas);
                    huboCambios = true;
                    factorizadoEnEsteNT = true;
                }

            } while (factorizadoEnEsteNT);
        }

        if (huboCambios) {
            ObservableList<String> nuevoNoTerm = FXCollections.observableArrayList(noTermModel);
            ObservableList<String> nuevasProducciones = FXCollections.observableArrayList();
            for (String nt : noTermModel) {
                for (List<String> alt : produccionesPorNT.getOrDefault(nt, Collections.emptyList())) {
                    String rhs = alt.isEmpty() ? "ε" : String.join(" ", alt);
                    nuevasProducciones.add(nt + " → " + rhs);
                }
            }
            this.setNoTerminalesModel(nuevoNoTerm);
            this.setProduccionesModel(nuevasProducciones);
        }

        return huboCambios;
    }

    // Helpers
    private String generarNombreNoTerminalUnico(String base, Set<String> existentes) {
        if (!existentes.contains(base)) return base;
        // Probar con más comillas primas, luego con números si es necesario
        String intento = base;
        while (existentes.contains(intento)) {
            intento = intento + "'";
            if (intento.length() > base.length() + 5) {
                // fallback con índice
                int idx = 1;
                intento = base + idx;
                while (existentes.contains(intento)) {
                    idx++;
                    intento = base + idx;
                }
                break;
            }
        }
        return intento;
    }

    private int longitudPrefijoComun(List<String> a, List<String> b) {
        int n = Math.min(a.size(), b.size());
        int i = 0;
        while (i < n && Objects.equals(a.get(i), b.get(i))) i++;
        return i;
    }

    /**
     * Verifica si la gramática tiene recursividad por la izquierda sin modificarla
     * @return true si tiene recursividad por la izquierda
     */
    public boolean verificarRecursividadSinModificar() {
        ObservableList<String> producciones = getProduccionesModel();

        for (String produccionStr : producciones) {
            String[] partesFlecha = produccionStr.split(" → ");
            if (partesFlecha.length != 2) {
                continue;
            }

            String antecedente = partesFlecha[0].trim();
            String consecuente = partesFlecha[1].trim();
            String[] simbolos = consecuente.split("\\s+");

            if (simbolos.length > 0 && antecedente.equals(simbolos[0])) {
                return true; // Encontró recursividad por la izquierda
            }
        }
        return false;
    }

    /**
     * Verifica si la gramática necesita factorización sin modificarla
     * @return true si necesita factorización
     */
    public boolean verificarFactorizacionSinModificar() {
        Map<String, List<String>> produccionesPorPrefijo = new HashMap<>();
        ObservableList<String> producciones = getProduccionesModel();

        for (String produccionStr : producciones) {
            String[] partesFlecha = produccionStr.split(" → ");
            if (partesFlecha.length != 2) {
                continue;
            }

            String antecedente = partesFlecha[0].trim();
            String consecuente = partesFlecha[1].trim();
            String[] simbolos = consecuente.split("\\s+");

            if (simbolos.length > 0) {
                String primerSimbolo = simbolos[0];

                // Agrupar producciones por antecedente y primer símbolo
                String clave = antecedente + "->" + primerSimbolo;
                produccionesPorPrefijo.computeIfAbsent(clave, k -> new ArrayList<>()).add(produccionStr);
            }
        }

        // Verificar si algún prefijo tiene más de una producción
        for (List<String> produccionesGrupo : produccionesPorPrefijo.values()) {
            if (produccionesGrupo.size() > 1) {
                return true; // Necesita factorización
            }
        }

        return false;
    }


    /**
     * Método para probar y demostrar el proceso completo de transformación de gramáticas
     * para análisis descendente predictivo
     */
    public void demostrarTransformaciones() {
        System.out.println("=== DEMOSTRACIÓN DE TRANSFORMACIONES PARA ANÁLISIS DESCENDENTE ===");
        System.out.println("Gramática original:");
        for (String prod : getProduccionesModel()) {
            System.out.println("  " + prod);
        }

        // Verificar estado inicial
        boolean tieneRecursividadInicial = verificarRecursividadSinModificar();
        boolean necesitaFactorizacionInicial = verificarFactorizacionSinModificar();

        System.out.println("\nEstado inicial:");
        System.out.println("  - Tiene recursividad por la izquierda: " + tieneRecursividadInicial);
        System.out.println("  - Necesita factorización: " + necesitaFactorizacionInicial);

        // Paso 1: Eliminar recursividad por la izquierda
        boolean seEliminoRecursividad = eliminarRecursividad();
        System.out.println("\nDespués de eliminar recursividad:");
        if (seEliminoRecursividad) {
            System.out.println("  ✅ Recursividad eliminada");
            for (String prod : getProduccionesModel()) {
                System.out.println("  " + prod);
            }
        } else {
            System.out.println("  ℹ️  No había recursividad que eliminar");
        }

        // Paso 2: Factorizar si es necesario
        boolean seFactorizo = factorizar();
        System.out.println("\nDespués de factorización:");
        if (seFactorizo) {
            System.out.println("  ✅ Factorización aplicada");
            for (String prod : getProduccionesModel()) {
                System.out.println("  " + prod);
            }
        } else {
            System.out.println("  ℹ️  No era necesaria la factorización");
        }

        // Verificar estado final
        boolean tieneRecursividadFinal = verificarRecursividadSinModificar();
        boolean necesitaFactorizacionFinal = verificarFactorizacionSinModificar();

        System.out.println("\nEstado final:");
        System.out.println("  - Tiene recursividad por la izquierda: " + tieneRecursividadFinal);
        System.out.println("  - Necesita factorización: " + necesitaFactorizacionFinal);

        if (!tieneRecursividadFinal && !necesitaFactorizacionFinal) {
            System.out.println("\n🎉 ¡GRAMÁTICA LISTA PARA ANÁLISIS DESCENDENTE PREDICTIVO!");
        } else {
            System.out.println("\n⚠️  La gramática aún necesita transformaciones adicionales");
        }

        System.out.println("=================================================================\n");
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
