package de.muenchen.allg.itd51.wollmux;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class DelayedUpdater
{
  public static final int UPDATE_DELAY = 400;

  public static void updateLater(DelayedUpdateable updateable)
  {
    if (updater == null) updater = new DelayedUpdater();
    updater.doUpdateLater(updateable);
  }

  public static void updateDone(DelayedUpdateable updateable)
  {
    if (updater != null) updater.doUpdateDone(updateable);
  }

  public static interface DelayedUpdateable
  {
    public void updateLater();
  }

  private static DelayedUpdater updater = null;

  private Timer timer;

  private HashMap currentTasks;

  private DelayedUpdater()
  {
    this.timer = new Timer();
    this.currentTasks = new HashMap();
  }

  private void doUpdateLater(DelayedUpdateable updateable)
  {
    UpdateTask oldTask = (UpdateTask) currentTasks.remove(updateable);
    if (oldTask != null) oldTask.cancel();

    UpdateTask newTask = new UpdateTask(updateable);
    currentTasks.put(updateable, newTask);
    try
    {
      timer.schedule(newTask, UPDATE_DELAY);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  private void doUpdateDone(DelayedUpdateable updateable)
  {
    currentTasks.remove(updateable);
  }

  private static class UpdateTask extends TimerTask
  {

    private DelayedUpdateable updateable;

    private UpdateTask(DelayedUpdateable updateable)
    {
      this.updateable = updateable;
    }

    public void run()
    {
      WollMuxEventHandler.handleUpdateLater(updateable);
    }
  }
}
