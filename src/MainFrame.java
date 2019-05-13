import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;

public class MainFrame extends JFrame {
    private JPanel rootPane;
    private JTextArea textArea1;
    private JScrollPane jScrollPane;
    private JScrollPane rowPane;
    private JTextArea textArea2;

    private JFileChooser jFileChooser;
    private Thread thread;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private InputStreamReader inputStreamReader;
    private OutputStreamWriter outputStreamWriter;
    private int lineNumber = 1;
    private MouseMenu mouseMenu;
    //Меню файла
    private JMenuItem exitButtonBar;
    private JMenuItem openButtonBar;
    private JMenuItem saveButtonBar;
    private JMenuItem createButtonBar;
    private JMenuItem saveAsButtonbar;
    //Меню вставки
    private JMenuItem undoButtonBar;
    private JMenuItem redoButtonBar;
    private JMenuItem cutButtonBar;
    private JMenuItem copyButtonBar;
    private JMenuItem pasteButtonBar;
    private JMenuItem deleteButtonBar;
    private JMenuItem highLightAllButtonBar;
    //Менеджер действий
    private UndoManager undoManager;

    private JMenu fileMenu;
    private JMenuBar menuBar;
    private JMenu editMenu;
    static final int BUFFER_SIZE = 50_0000;
    private File file;
    private DefaultCaret defaultCaret;
    private Clipboard clipboard;
    private StringSelection stringSelection;

    {
        //Настройка mainFrame
        setContentPane(rootPane);
        Dimension dimension = new Dimension(600, 300);
        setMinimumSize(dimension);
        setPreferredSize(dimension);
        setLocationRelativeTo(null);
        //Настройки для окна вывода текста
        textArea1.setEditable(true);
        textArea1.setLineWrap(true);
        textArea1.setWrapStyleWord(true);
        undoManager = new UndoMergeManager(textArea1);
        undoManager.setLimit(20);

        //Отмена автопрокрутки у текстового окна
        defaultCaret = (DefaultCaret) textArea1.getCaret();
        defaultCaret.setUpdatePolicy(DefaultCaret.OUT_BOTTOM);
        //Блокировка скролбаров поля вывода номеров строк
        rowPane.getVerticalScrollBar().setEnabled(false);
        rowPane.getHorizontalScrollBar().setEnabled(false);
        rowPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        rowPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        //Блокировка редактирования поля столбцов
        textArea2.setEditable(false);
        //Синхронизация скролбаров текстового поля и поля номеров строк
        rowPane.getVerticalScrollBar().setModel(jScrollPane.getVerticalScrollBar().getModel());
        //Убираем грацниы поля для строк
        rowPane.setBorder(null);
        //Отмена автопрокрутки поля для строк
        DefaultCaret rowPaneCaret = (DefaultCaret) textArea2.getCaret();
        rowPaneCaret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        textArea2.setFont(textArea1.getFont());
        mouseMenu = new MouseMenu();
        //Меню "файл"
        fileMenu = new JMenu("Файл");
        openButtonBar = new JMenuItem("Открыть...");
        saveButtonBar = new JMenuItem("Сохранить");
        saveAsButtonbar = new JMenuItem("Сохранить как..");
        exitButtonBar = new JMenuItem("Выход");
        createButtonBar = new JMenuItem("Создать");

        fileMenu.add(createButtonBar);
        fileMenu.add(openButtonBar);
        fileMenu.add(saveButtonBar);
        fileMenu.add(saveAsButtonbar);
        fileMenu.add(exitButtonBar);
        saveButtonBar.setEnabled(false);
        fileMenu.setVisible(true);
        menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        //Меню "Правка"
        editMenu = new JMenu("Правка");
        undoButtonBar = new JMenuItem("Отменить");
        redoButtonBar = new JMenuItem("Повторить");
        cutButtonBar = new JMenuItem("Вырезать");
        copyButtonBar = new JMenuItem("Копировать");
        pasteButtonBar = new JMenuItem("Вставить");
        deleteButtonBar = new JMenuItem("Удалить");
        highLightAllButtonBar = new JMenuItem("Выделить всё");
        editMenu.add(undoButtonBar);
        editMenu.add(redoButtonBar);
        editMenu.add(cutButtonBar);
        editMenu.add(copyButtonBar);
        editMenu.add(pasteButtonBar);
        editMenu.add(deleteButtonBar);
        editMenu.add(highLightAllButtonBar);
        menuBar.add(editMenu);

        //Доступ к системному буферу обмена, для копирования и вставки вне программы
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();


        jFileChooser = new JFileChooser() {
            //Переопределяем метод в классе, для вызова окна в случае существования файла с таким именем при сохранении
            @Override
            public void approveSelection() {
                File checkFile = new File(jFileChooser.getSelectedFile().getAbsolutePath());
                if (checkFile.exists() && getDialogType() == SAVE_DIALOG) {
                    int result = JOptionPane.showConfirmDialog(this,"Файл с таким именем уже существует" +
                            " перезаписать?","Перезапись файла",JOptionPane.YES_NO_CANCEL_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        super.approveSelection();
                        return;
                    } else if (result == JOptionPane.NO_OPTION) {
                        return;
                    } else if (result == JOptionPane.CANCEL_OPTION) {
                        return;
                    } else if (result == JOptionPane.CLOSED_OPTION) {
                        return;
                    }
                }
                super.approveSelection();
            }
        };
        //Устанавливаем фильтр, чтобы файлы записывались в txt формате
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
        jFileChooser.setDialogTitle("Выбрать файл");
        jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jFileChooser.setCurrentDirectory(new File("."));
        jFileChooser.setFileFilter(filter);
        jFileChooser.setMultiSelectionEnabled(false);
        jFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    }

    public MainFrame() {

        exitButtonBar.addActionListener(e -> {
            if (thread != null) {
                thread.interrupt();
            }
            System.exit(WindowConstants.EXIT_ON_CLOSE);
        });

        createButtonBar.addActionListener(e -> {
            textArea1.replaceRange("", 0, textArea1.getText().length());
            saveButtonBar.setEnabled(false);
        });

        //TODO написать обработку кликов мыши по кнопкам
        //Метод открытия меню мыши
        textArea1.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (!mouseMenu.isVisible()) {
                        mouseMenu.setLocation((int) MouseInfo.getPointerInfo().getLocation().getX(), (int) MouseInfo.getPointerInfo().getLocation().getY());
                        mouseMenu.setVisible(true);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        //Метод закрытия меню мыши при клике не туда
        mouseMenu.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {

            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                mouseMenu.setVisible(false);
            }
        });


        textArea1.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    textArea2.append(lineNumber++ + "\n");
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        openButtonBar.addActionListener(e -> {

            if (thread != null) {
                    thread=null;
                    //При открытии нового файла очищаем поля
                    textArea1.replaceRange("", 0, textArea1.getText().length());
                    textArea2.replaceRange("", 0, textArea2.getText().length());
                    //Обнуляем bufferedReader и InputStreamReader, во избежании некорректного вывода
                    bufferedReader = null;
                    inputStreamReader = null;
                    lineNumber = 1;
                }
                //Открываем окно выбора файла
            jFileChooser.setVisible(true);
            int returnVal = jFileChooser.showOpenDialog(MainFrame.this);
            if (returnVal == jFileChooser.APPROVE_OPTION) {
                //Получаем выбранный файл и открываем
                file = jFileChooser.getSelectedFile();
                thread = new Thread(() -> {
                        //Создаём bufferedReader для считывания файла и считываем первые 30* строк с выводом, * - количество зависит от размера шрифта и размера текстового окна
                            //TODO разобраться с кодировкой UTF-8
                            //TODO написать прокрутку файла с выводом последующих строк
                    jScrollPane.getVerticalScrollBar().getAccessibleContext().addPropertyChangeListener(new PropertyChangeListener() {

                        int linesInArea = textArea1.getHeight() / textArea1.getFont().getSize();
                        int previousScrollbarPosition;
                        {
                            try {
                                inputStreamReader = new InputStreamReader(new FileInputStream(file));
                                bufferedReader = new BufferedReader(inputStreamReader, BUFFER_SIZE);
                                //Считываем первые строки файла
                                readLine(bufferedReader, linesInArea);
                                saveButtonBar.setEnabled(true);
                            } catch (Exception d) {
                                JOptionPane.showMessageDialog(MainFrame.this, "Ошибка при открытии файда", "Ошибка!", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        @Override
                        public void propertyChange (PropertyChangeEvent evt){
                            int currentScrollBarPosition = jScrollPane.getVerticalScrollBar().getValue();
                            if (currentScrollBarPosition > previousScrollbarPosition) {
                                try {
                                    //Постепенно подгружаем строки по мере прокрутки скролбара
                                    if (previousScrollbarPosition == 0) {
                                        readLine(bufferedReader, 1);
                                    } else if (currentScrollBarPosition%previousScrollbarPosition>=5) {
                                        readLine(bufferedReader, 1);
                                    }

                                } catch (IOException dd) {
                                    dd.printStackTrace();
                                }
                            }
                            previousScrollbarPosition = currentScrollBarPosition;
                        }




                        void readLine(BufferedReader bufferedReader, int linesInArea) throws IOException {
                            String line;
                            for (int i = 0; i < linesInArea; i++) {
                                if ((line = bufferedReader.readLine())!=null) {
                                    textArea1.append(line + "\n");
                                    textArea2.append((lineNumber++) + "\n");
                                }
                            }
                        }
                    });
                });
                thread.run();
            }
        });

        saveButtonBar.addActionListener(e -> {
            saveFile();
        });

        saveAsButtonbar.addActionListener(e -> {
            saveFileAs();
        });

        //Кнопки выделить всё
        ActionListener highLightAll = e -> {
            textArea1.selectAll();
            mouseMenu.setVisible(false);
        };
        mouseMenu.setHighlightButton(highLightAll);
        highLightAllButtonBar.addActionListener(highLightAll);

        //Кнопки копирования
        ActionListener copyMethod = e -> copyToClipBoard();
        mouseMenu.setCopyButton(copyMethod);
        copyButtonBar.addActionListener(copyMethod);
        //Кнопки вырезания
        ActionListener cutMethod = e -> {
            copyToClipBoard();
            deleteSelectedText();
        };
        mouseMenu.setCutButton(cutMethod);
        cutButtonBar.addActionListener(cutMethod);
        //Кнопки вставки
        ActionListener insertMethod = e -> {
            try {
                if (String.valueOf(clipboard.getData(DataFlavor.stringFlavor)) !=null) {
                    textArea1.insert(String.valueOf(clipboard.getData(DataFlavor.stringFlavor)), textArea1.getCaretPosition());
                }
            } catch (IOException | UnsupportedFlavorException c) {
                c.printStackTrace();
            } finally {
                if (mouseMenu.isVisible()) {
                    mouseMenu.setVisible(false);
                }
            }
        };
        mouseMenu.setInsertButton(insertMethod);
        pasteButtonBar.addActionListener(insertMethod);

        //Кнопки удаления
        ActionListener deleteMethod = e -> {
            if (mouseMenu.isVisible()) {
                mouseMenu.setVisible(false);
            }
            deleteSelectedText();
        };
        mouseMenu.setDeleteButton(deleteMethod);
        deleteButtonBar.addActionListener(deleteMethod);

        //Кнопки отмены действий
        ActionListener undoMethod = e -> {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
            if (mouseMenu.isVisible()) {
                mouseMenu.setVisible(false);
            }
        };
        mouseMenu.setUndoButton(undoMethod);
        undoButtonBar.addActionListener(undoMethod);

        //Кнопки повтора действий
        ActionListener redoMethod = e -> {
            if (undoManager.canRedo()) {
                undoManager.redo();
            }
            if (mouseMenu.isVisible()) {
                mouseMenu.setVisible(false);
            }
        };
        mouseMenu.setRedoButton(redoMethod);
        redoButtonBar.addActionListener(redoMethod);

    }

    //Метод удаления выделенного текста
    private void deleteSelectedText() {
        textArea1.replaceRange("", textArea1.getSelectionStart(), textArea1.getSelectionEnd());
    }

    //Копирование в буфер обмена
    private void copyToClipBoard() {
        if (textArea1.getSelectedText() != null) {
            stringSelection = new StringSelection(textArea1.getSelectedText());
            clipboard.setContents(stringSelection, null);
            mouseMenu.setVisible(false);
        } else {
            mouseMenu.setVisible(false);
        }
    }

    //Метод сохранения файлов
    private void saveFile(){
        try {
            if (file != null) {
                outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
                bufferedWriter = new BufferedWriter(outputStreamWriter, BUFFER_SIZE);
                bufferedWriter.write(textArea1.getText());
                outputStreamWriter.flush();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Метод сохранения "сохранить как"
    private void saveFileAs() {
        int returnVal = jFileChooser.showSaveDialog(MainFrame.this);
        jFileChooser.setVisible(true);
        if (returnVal == jFileChooser.APPROVE_OPTION) {
            if (jFileChooser.getSelectedFile().exists()) {
                file = jFileChooser.getSelectedFile();
            } else {
                file = new File(jFileChooser.getSelectedFile() + ".txt");
            }
            saveFile();
        }
    }
}
