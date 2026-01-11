package com.goldsprite.gdengine.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Collections;

public class CommandManager {
	private final Stack<ICommand> undoStack = new Stack<>();
	private final Stack<ICommand> redoStack = new Stack<>();
	private final List<CommandListener> listeners = new ArrayList<>();

	private int maxHistorySize = 50;

	public interface CommandListener {
		default void onCommandExecuted(ICommand cmd) {}
		default void onUndo(ICommand cmd) {}
		default void onRedo(ICommand cmd) {}
		default void onHistoryChanged() {}
		default void onHistoryNavigated(int position) {}
	}

	public void execute(ICommand cmd) {
		// 先执行，再入栈
		// 注意：有些场景下 Command 可能在外部已经被执行了（比如实时拖拽），
		// 这种情况下 Command 的 execute 应该设计为幂等，或者由调用者决定是否只 push 不 execute。
		// 但为了统一，标准模式是：创建 Command -> 传给 Manager -> Manager 调用 execute。
		// 对于 Gizmo 拖拽这种连续操作，我们在拖拽结束时生成一个 Command，
		// 这个 Command 的 execute() 可以是“应用最终值”，也可以是“什么都不做（因为值已经设了）”。
		// 为了支持 Redo，execute() 必须包含设置新值的逻辑。
		// 如果我们在拖拽结束时已经设好了新值，再次调用 execute() 设置相同的值是无害的。

		cmd.execute();

		undoStack.push(cmd);
		redoStack.clear();

		if (undoStack.size() > maxHistorySize) {
			undoStack.remove(0);
		}

		notifyExecuted(cmd);
		notifyHistoryChanged();
	}

	public void undo() {
		if (undoStack.isEmpty()) return;
		ICommand cmd = undoStack.pop();
		cmd.undo();
		redoStack.push(cmd);

		notifyUndo(cmd);
		notifyHistoryChanged();
	}

	public void redo() {
		if (redoStack.isEmpty()) return;
		ICommand cmd = redoStack.pop();
		cmd.execute();
		undoStack.push(cmd);

		notifyRedo(cmd);
		notifyHistoryChanged();
	}

	public boolean canUndo() { return !undoStack.isEmpty(); }
	public boolean canRedo() { return !redoStack.isEmpty(); }
	public int getUndoStackSize() { return undoStack.size(); }
	public int getRedoStackSize() { return redoStack.size(); }

	/**
	 * 获取undo栈中的所有命令的副本
	 * @return 包含所有命令的列表，最早执行的命令在列表开头
	 */
	public List<ICommand> getUndoCommands() {
		// 返回undo栈的副本，保持原始顺序（最早执行的命令在列表开头）
		List<ICommand> result = new ArrayList<>(undoStack);
		Collections.reverse(result); // 反转列表，使最早的命令在开头
		return result;
	}

	/**
	 * 获取所有历史命令（包括undo和redo栈中的命令）
	 * @return 包含所有命令的列表，按执行顺序排列
	 */
	public List<ICommand> getAllHistoryCommands() {
		List<ICommand> result = new ArrayList<>();

		// 添加undo栈中的命令（最早执行的命令在列表开头）
		List<ICommand> undoCommands = new ArrayList<>(undoStack);
		Collections.reverse(undoCommands);
		result.addAll(undoCommands);

		// 添加redo栈中的命令（按重做顺序）
		List<ICommand> redoCommands = new ArrayList<>(redoStack);
		Collections.reverse(redoCommands); // 反转redo栈，使最早撤销的命令在前面
		result.addAll(redoCommands);

		return result;
	}

	/**
	 * 获取当前 Undo 栈顶的命令（即最近执行的命令）。
	 * 用于状态比对（如检测文件是否变脏）。
	 * @return 栈顶命令，如果栈为空则返回 null
	 */
	public ICommand getLastCommand() {
		return undoStack.isEmpty() ? null : undoStack.peek();
	}

	public void clear() {
		undoStack.clear();
		redoStack.clear();
		notifyHistoryChanged();
	}

	public void addListener(CommandListener l) { listeners.add(l); }
	public void removeListener(CommandListener l) { listeners.remove(l); }

	private void notifyExecuted(ICommand cmd) {
		for (CommandListener l : listeners) l.onCommandExecuted(cmd);
	}
	private void notifyUndo(ICommand cmd) {
		for (CommandListener l : listeners) l.onUndo(cmd);
	}
	private void notifyRedo(ICommand cmd) {
		for (CommandListener l : listeners) l.onRedo(cmd);
	}
	/**
	 * 跳转到指定的历史节点位置
	 * @param position 目标位置（0表示最早的历史，undoStack.size()-1表示最近的历史）
	 */
	public void navigateToHistory(int position) {
		if (position < 0 || position >= undoStack.size() + redoStack.size()) return;

		int currentPosition = undoStack.size() - 1;

		// 如果目标位置在当前节点之前（需要撤销）
		if (position < currentPosition) {
			// 计算需要撤销的步数
			int stepsToUndo = currentPosition - position;

			// 执行撤销操作直到到达目标位置
			for (int i = 0; i < stepsToUndo; i++) {
				ICommand cmd = undoStack.pop();
				cmd.undo();
				redoStack.push(cmd);
				notifyUndo(cmd);
			}
		}
		// 如果目标位置在当前节点之后（需要重做）
		else if (position > currentPosition) {
			// 计算需要重做的步数
			int stepsToRedo = position - currentPosition;

			// 执行重做操作直到到达目标位置
			for (int i = 0; i < stepsToRedo; i++) {
				ICommand cmd = redoStack.pop();
				cmd.execute();
				undoStack.push(cmd);
				notifyRedo(cmd);
			}
		}

		notifyHistoryChanged();
		notifyHistoryNavigated(position);
	}

	private void notifyHistoryChanged() {
		for (CommandListener l : listeners) l.onHistoryChanged();
	}

	private void notifyHistoryNavigated(int position) {
		for (CommandListener l : listeners) l.onHistoryNavigated(position);
	}
}
