package splitter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Spreadsheet {
    private HSSFSheet sheet;
    private String name;
    private String directory;
    private String subdirectory;
    private List<String> columns;

    public Spreadsheet(String name){
        HSSFWorkbook workbook = new HSSFWorkbook();
        this.sheet = workbook.createSheet();
        this.name = name;
        this.directory = "";
        this.subdirectory = "";
    }

    public Spreadsheet(String name, String directory){
        HSSFWorkbook workbook = new HSSFWorkbook();
        this.sheet = workbook.createSheet();
        this.name = name;
        this.directory = directory;
        this.subdirectory = "";
    }

    public Spreadsheet(String name, String directory, String subdirectory){
        HSSFWorkbook workbook = new HSSFWorkbook();
        this.sheet = workbook.createSheet();
        this.name = name;
        this.directory = directory;
        this.subdirectory = subdirectory;
    }

    public Spreadsheet(File file) throws IOException {
        FileInputStream f = new FileInputStream(file);
        NPOIFSFileSystem fs = new NPOIFSFileSystem(f);
        HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true);
        sheet = wb.getSheetAt(0);
        fs.close();
        f.close();

        if(!isFormatCorrect()){
            IOException e = new IOException();
            throw e;
        }

        this.name = FilenameUtils.removeExtension(file.getName());
        this.directory = file.getParent();

        this.columns = new ArrayList<>();

        for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++){
            this.columns.add(i, getCellValue(0, i));
        }
    }

    private boolean isFormatCorrect() {
        if (this.sheet.getRow(0) == null) {
            return false;
        }
        return true;
    }

    public HSSFSheet getSheet(){
        return this.sheet;
    }

    public String getName(){
        return this.name;
    }

    public String getDirectory(){
        return this.directory;
    }

    public String getSubdirectory() {
        return subdirectory;
    }

    public List<String> getColumns(){
        return this.columns;
    }

    public String getCellValue(int row, int col){
        DataFormatter df = new DataFormatter();
        return df.formatCellValue(this.sheet.getRow(row).getCell(col));
    }

    public void copyRow(Spreadsheet s2, int row1, int row2){
        int size = this.sheet.getRow(0).getPhysicalNumberOfCells();
        HSSFRow row = s2.getSheet().createRow(row2);

        String cellValue;
        for(int i = 0; i < size; i++){
            HSSFCell cell = row.createCell(i);
            cellValue = this.getCellValue(row1, i);
            if (StringUtils.isNumeric(cellValue)){
                cell.setCellValue(Double.parseDouble(cellValue));
            } else {
                cell.setCellValue(cellValue);
            }
        }
    }

    public void addRow(Spreadsheet s2, int row1){
        copyRow(s2, row1, s2.getSheet().getPhysicalNumberOfRows());
    }

    public List<Spreadsheet> split(int directoryCol, int subdirectoryCol, int column){
        List<Spreadsheet> spreadsheets = new ArrayList<>();
        List<Triple> tripleList = new ArrayList<>();
        int size = this.sheet.getPhysicalNumberOfRows();

        for (int i = 1; i < size; i++){
            String cellValue = this.getCellValue(i, column);
            String dirValue = "";
            String subdirValue = "";
            if (directoryCol >= 0){
                dirValue = this.getCellValue(i, directoryCol);
                if (subdirectoryCol >= 0){
                    subdirValue = this.getCellValue(i, subdirectoryCol);
                }
            }
            Triple t = new Triple(dirValue, subdirValue, cellValue);
            if (!tripleList.contains(t)){
                tripleList.add(t);
                if (directoryCol >= 0 && subdirectoryCol >= 0){
                    spreadsheets.add(new Spreadsheet(this.getName() + "_" + cellValue, this.getCellValue(i, directoryCol), this.getCellValue(i, subdirectoryCol)));
                } else if (directoryCol >= 0 && subdirectoryCol < 0){
                    spreadsheets.add(new Spreadsheet(this.getName() + "_" + cellValue, this.getCellValue(i, directoryCol)));
                } else if (directoryCol < 0 && subdirectoryCol < 0){
                    spreadsheets.add(new Spreadsheet(this.getName() + "_" + cellValue));
                }
            }
        }

        for(int i = 0; i < tripleList.size(); i++){
            this.addRow(spreadsheets.get(i), 0);
            for(int j = 0; j < size; j++){
                if (directoryCol >= 0 && subdirectoryCol >= 0) {
                    if(this.getCellValue(j, directoryCol).equals(tripleList.get(i).first) && this.getCellValue(j, subdirectoryCol).equals(tripleList.get(i).second) && this.getCellValue(j, column).equals(tripleList.get(i).third)){
                        this.addRow(spreadsheets.get(i), j);
                    }
                } else if (directoryCol >= 0 && subdirectoryCol < 0){
                    if(this.getCellValue(j, directoryCol).equals(tripleList.get(i).first) && this.getCellValue(j, column).equals(tripleList.get(i).third)){
                        this.addRow(spreadsheets.get(i), j);
                    }
                } else if (directoryCol < 0 && subdirectoryCol < 0){
                    if(this.getCellValue(j, column).equals(tripleList.get(i).third)){
                        this.addRow(spreadsheets.get(i), j);
                    }
                }
            }
        }

        return spreadsheets;
    }

    public void autoSizeColumns() {
        if (this.sheet.getPhysicalNumberOfRows() > 0) {
            Row row = sheet.getRow(0);
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                int columnIndex = cell.getColumnIndex();
                sheet.autoSizeColumn(columnIndex);
            }
        }
    }

    public void export(String filename) throws IOException{
        HSSFWorkbook workbook = this.sheet.getWorkbook();
        workbook.write(new FileOutputStream(filename + ".xls"));
        workbook.close();
    }
}

class Triple<F, S, T> {

    public final F first;
    public final S second;
    public final T third;

    public Triple(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Triple)) {
            return false;
        }
        Triple<?, ?, ?> p = (Triple<?, ?, ?>) o;
        return first.equals(p.first) && second.equals(p.second) && third.equals(p.third);
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode()) ^ (third == null ? 0 : third.hashCode());
    }

    public static <F, S, T> Triple <F, S, T> create(F f, S s, T t) {
        return new Triple<F, S, T>(f, s, t);
    }
}

