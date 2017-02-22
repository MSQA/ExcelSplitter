package splitter;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class ExportController extends ExcelController implements Initializable{
    @FXML
    private Button backButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button chooseDirectoryButton;
    @FXML
    private Label directoryLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private CheckBox checkBox;
    private IndexedString indexedString;
    private String dir;
    private String finalPath;
    private IndexedString directory;
    private IndexedString subdirectory;

    public void handleChooseDirectory(ActionEvent actionEvent) {
        Node source = (Node) actionEvent.getSource();
        Window stage = source.getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if(selectedDirectory == null){
            directoryLabel.setText(this.getSpreadsheet().getDirectory());
        }else{
            directoryLabel.setText(selectedDirectory.getAbsolutePath());
            dir = selectedDirectory.getAbsolutePath();
        }
    }

    public void handleExportButton(ActionEvent actionEvent) {
        //Checking if I can write in the chosen directory
        File f = new File(dir);
        Path p = f.toPath();
        if (Files.isWritable(p)) {
            this.progressBar.setVisible(true);
            checkBox.setDisable(true);
            backButton.setDisable(true);
            exportButton.setDisable(true);
            cancelButton.setDisable(true);
            chooseDirectoryButton.setDisable(true);

            //prevent from closing during file export
            Stage stage =(Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
            Platform.setImplicitExit(false);
            stage.setOnCloseRequest(event -> event.consume());

            final Spreadsheet spreadsheet = this.getSpreadsheet();
            final String previousFxml = this.getPreviousFxml();

            Task<Void> task = new Task<Void>() {

                @Override
                protected Void call() throws Exception {
                    List<Spreadsheet> spreadsheetList = null;
                    String path = dir;
                    if (checkBox.isSelected()){
                        path += File.separator + "Excel Splitter";
                    }
                    if (previousFxml.equals("../fxml/advanced_split.fxml")){
                        spreadsheetList  = Spreadsheet.split(spreadsheet, directory.getIndex(), subdirectory.getIndex(), indexedString.getIndex());
                    } else if (previousFxml.equals("../fxml/split.fxml")) {
                        spreadsheetList = Spreadsheet.split(spreadsheet, -1, -1, indexedString.getIndex());
                    }

                    finalPath = path;
                    File f;

                    for (int i = 0; i < spreadsheetList.size(); i++) {
                        String dirString = spreadsheetList.get(i).getDirectory();
                        dirString = formatName(dirString);

                        String subdirString = spreadsheetList.get(i).getSubdirectory();
                        subdirString = formatName(subdirString);

                        f = new File(path + File.separator + dirString + File.separator + subdirString);
                        if (!(f.exists() && f.isDirectory())){
                            f.mkdirs();
                        }

                        String name = spreadsheetList.get(i).getName();
                        name = formatName(name);

                        String fullFilePath = path + File.separator + dirString + File.separator + subdirString + File.separator + name;

                        recursiveCheckedExport(actionEvent, spreadsheetList.get(i), fullFilePath, 0);

                        updateProgress(i + 1, spreadsheetList.size());
                    }

                    return null;
                }

            };
            task.setOnSucceeded(e -> {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../fxml/finish.fxml"));
                try {
                    Parent root = fxmlLoader.load();
                    FinishController controller = fxmlLoader.getController();
                    controller.setDirectory(finalPath);
                    Stage st_old =(Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
                    Stage st_new = new Stage();
                    st_new.setResizable(false);
                    st_new.getIcons().add(new Image(Main.class.getResourceAsStream("../res/excel-splitter-small.png")));
                    st_new.setTitle("Excel Splitter");
                    Scene scene = new Scene(root);
                    scene.getStylesheets().add("css/theme.css");
                    st_new.setScene(scene);
                    st_new.setX(st_old.getX());
                    st_new.setY(st_old.getY());
                    st_old.close();
                    st_new.show();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            progressBar.progressProperty().bind(task.progressProperty());
            new Thread(task).start();
        } else {
            errorDialog(((Node)actionEvent.getSource()).getScene().getWindow(), "Non ho i permessi per esportare in questa cartella!");
        }
    }

    private String formatName(String name){
        name = name.replaceAll(" ", "_");
        name = name.replaceAll("/", "_");
        name = name.replaceAll(",", "");
        name = name.replaceAll(":", "");
        name = name.replaceAll("<", "_");
        name = name.replaceAll(">", "_");
        name = name.replaceAll("\"", "");
        name = name.replaceAll("\\\\", "");
        name = name.replaceAll("|", "");
        name = name.replaceAll("\\?", "");
        name = name.replaceAll("\\*", "");
        if(name.length() > 128){
            name = name.substring(0, 128);
            name += "~";
        }
        return name;
    }

    private void recursiveCheckedExport(ActionEvent actionEvent, Spreadsheet s, String path, int count){
        Path p;
        if (count == 0) {
            p = Paths.get(path + ".xls");
        } else {
            p = Paths.get(path + count + ".xls");
        }
        boolean exists = Files.exists(p);
        boolean notExists = Files.notExists(p);

        if (exists) {
            recursiveCheckedExport(actionEvent, s, path, ++count);
        } else if (notExists) {
            if (count == 0){
                try {
                    s.export(path);
                } catch (IOException e) {
                    errorDialog(((Node)actionEvent.getSource()).getScene().getWindow(), "Errore: impossibile esportare il file " + s.getName() + ".xls");
                    e.printStackTrace();
                }
            } else {
                try {
                    s.export(path + count);
                } catch (IOException e) {
                    errorDialog(((Node)actionEvent.getSource()).getScene().getWindow(), "Errore: impossibile esportare il file " + s.getName() + ".xls");
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("File's status is unknown!");
        }
    }

    @Override
    public void setSpreadsheet(Spreadsheet spreadsheet) {
        super.setSpreadsheet(spreadsheet);
        this.dir = spreadsheet.getDirectory();
        directoryLabel.setText(dir);
    }

    public void setIndexedString(IndexedString indexedString){
        this.indexedString = indexedString;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.setNextFxml("../fxml/finish.fxml");
        this.progressBar.setVisible(false);
    }

    public void setDirectory(IndexedString directory) {
        this.directory = directory;
    }

    public void setSubdirectory(IndexedString subdirectory) {
        this.subdirectory = subdirectory;
    }
}
