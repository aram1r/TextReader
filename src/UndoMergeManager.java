import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.undo.*;


// Необходим для объединения вводимых символов в группы для операции отмены
// Код взят из открытых исчтоников
// http://www.camick.com/java/source/CompoundUndoManager.java

public class UndoMergeManager extends UndoManager
        implements UndoableEditListener, DocumentListener
{
    private UndoManager undoManager;
    private CompoundEdit compoundEdit;
    private JTextComponent textComponent;
    private UndoAction undoAction;
    private RedoAction redoAction;

    //  Данные поля позволяют определить ввод символо, оффсет и длина должны увеличиваться на 1 при вводе
    // новых символов или уменьшаться на 1 при удалении

    private int lastOffset;
    private int lastLength;

    public UndoMergeManager(JTextComponent textComponent)
    {
        this.textComponent = textComponent;
        undoManager = this;
        undoAction = new UndoAction();
        redoAction = new RedoAction();
        textComponent.getDocument().addUndoableEditListener( this );
    }

    /*
        Добавляем DocumentListener перед операцией отмены, чтобы корректно определить позицию каретки
     */
    public void undo()
    {
        textComponent.getDocument().addDocumentListener( this );
        super.undo();
        textComponent.getDocument().removeDocumentListener( this );
    }

    public void redo()
    {
        textComponent.getDocument().addDocumentListener( this );
        super.redo();
        textComponent.getDocument().removeDocumentListener( this );
    }

    /*
        Если выполняется метод UndoableEdit ввод будет поглощён текущим compound или стартанёт новый
     */
    public void undoableEditHappened(UndoableEditEvent e)
    {
        //  Стартуем новый CompoundEdit

        if (compoundEdit == null)
        {
            compoundEdit = startCompoundEdit( e.getEdit() );
            return;
        }

        int offsetChange = textComponent.getCaretPosition() - lastOffset;
        int lengthChange = textComponent.getDocument().getLength() - lastLength;

        //  Проверяем изменение атрибуты

        AbstractDocument.DefaultDocumentEvent event =
                (AbstractDocument.DefaultDocumentEvent)e.getEdit();

        if  (event.getType().equals(DocumentEvent.EventType.CHANGE))
        {
            if (offsetChange == 0)
            {
                compoundEdit.addEdit(e.getEdit() );
                return;
            }
        }

        //  Проверяем изменения в документе
        //  Изменения позиция каретки и длины документа должны совпадать и быть равны 1 или -1

//		int offsetChange = textComponent.getCaretPosition() - lastOffset;
//		int lengthChange = textComponent.getDocument().getLength() - lastLength;

        if (offsetChange == lengthChange
                &&  Math.abs(offsetChange) == 1)
        {
            compoundEdit.addEdit( e.getEdit() );
            lastOffset = textComponent.getCaretPosition();
            lastLength = textComponent.getDocument().getLength();
            return;
        }

        //  Not incremental edit, end previous edit and start a new one

        compoundEdit.end();
        compoundEdit = startCompoundEdit( e.getEdit() );
    }

    /*
     **  Each CompoundEdit will store a group of related incremental edits
     **  (ie. each character typed or backspaced is an incremental edit)
     */
    private CompoundEdit startCompoundEdit(UndoableEdit anEdit)
    {
        //  Track Caret and Document information of this compound edit

        lastOffset = textComponent.getCaretPosition();
        lastLength = textComponent.getDocument().getLength();

        //  The compound edit is used to store incremental edits

        compoundEdit = new MyCompoundEdit();
        compoundEdit.addEdit( anEdit );

        addEdit( compoundEdit );

        undoAction.updateUndoState();
        redoAction.updateRedoState();

        return compoundEdit;
    }

    public Action getUndoAction()
    {
        return undoAction;
    }

    /*
     *  The Action to Redo changes to the Document.
     *  The state of the Action is managed by the CompoundUndoManager
     */
    public Action getRedoAction()
    {
        return redoAction;
    }


    public void insertUpdate(final DocumentEvent e)
    {
        SwingUtilities.invokeLater(() -> {
            int offset = e.getOffset() + e.getLength();
            offset = Math.min(offset, textComponent.getDocument().getLength());
            textComponent.setCaretPosition( offset );
        });
    }

    public void removeUpdate(DocumentEvent e)
    {
        textComponent.setCaretPosition(e.getOffset());
    }

    public void changedUpdate(DocumentEvent e) {}


    class MyCompoundEdit extends CompoundEdit
    {
        public boolean isInProgress()
        {
            //  in order for the canUndo() and canRedo() methods to work
            //  assume that the compound edit is never in progress

            return false;
        }

        public void undo() throws CannotUndoException
        {
            //  End the edit so future edits don't get absorbed by this edit

            if (compoundEdit != null)
                compoundEdit.end();

            super.undo();

            //  Всегда создаём создаём новый compoundeditor после отмены

            compoundEdit = null;
        }
    }

    /*
     *	Выполняем отмену и обновляем состояние отмены/повтора действий
     */
    class UndoAction extends AbstractAction
    {
        public UndoAction()
        {
            putValue( Action.NAME, "Undo" );
            putValue( Action.SHORT_DESCRIPTION, getValue(Action.NAME) );
            putValue( Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_U) );
            putValue( Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control Z") );
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e)
        {
            try
            {
                undoManager.undo();
                textComponent.requestFocusInWindow();
            }
            catch (CannotUndoException ex) {}

            updateUndoState();
            redoAction.updateRedoState();
        }

        private void updateUndoState()
        {
            setEnabled( undoManager.canUndo() );
        }
    }

    /*
     *	Выполняем повтор и обновляем состояние отмены/повтора действий
     */
    class RedoAction extends AbstractAction
    {
        public RedoAction()
        {
            putValue( Action.NAME, "Redo" );
            putValue( Action.SHORT_DESCRIPTION, getValue(Action.NAME) );
            putValue( Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R) );
            putValue( Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK) );
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e)
        {
            try
            {
                undoManager.redo();
                textComponent.requestFocusInWindow();
            }
            catch (CannotRedoException ex) {}

            updateRedoState();
            undoAction.updateUndoState();
        }

        protected void updateRedoState()
        {
            setEnabled( undoManager.canRedo() );
        }
    }
}

