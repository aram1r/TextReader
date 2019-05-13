import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MouseMenu extends JFrame {
    private JPanel rootPane;
    private JButton undoButton;
    private JButton highlightAllButton;
    private JButton deleteButton;
    private JButton insertButton;
    private JButton copyButton;
    private JButton cutButton;
    private JButton redoButton;

    {
        setVisible(false);
        Dimension dimension = new Dimension(75, 230);
        setSize(dimension);
        setMinimumSize(dimension);
        setContentPane(rootPane);
        setLocationRelativeTo(null);
        setUndecorated(true);


        undoButton.setContentAreaFilled(false);
        highlightAllButton.setContentAreaFilled(false);
        deleteButton.setContentAreaFilled(false);
        insertButton.setContentAreaFilled(false);
        copyButton.setContentAreaFilled(false);
        cutButton.setContentAreaFilled(false);
        redoButton.setContentAreaFilled(false);
    }

    void setHighlightButton(ActionListener e) {
        highlightAllButton.addActionListener(e);
    }

    void setCopyButton(ActionListener e) {
        copyButton.addActionListener(e);
    }

    void setCutButton(ActionListener e) {
        cutButton.addActionListener(e);
    }
    void setInsertButton(ActionListener e) {
        insertButton.addActionListener(e);
    }

    void setDeleteButton(ActionListener e) {
        deleteButton.addActionListener(e);
    }

    void setUndoButton(ActionListener e) {
        undoButton.addActionListener(e);
    }

    void setRedoButton(ActionListener e) {
        redoButton.addActionListener(e);
    }


}
