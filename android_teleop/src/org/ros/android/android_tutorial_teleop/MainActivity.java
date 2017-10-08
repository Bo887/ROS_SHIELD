/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.android_tutorial_teleop;

import com.google.common.collect.Lists;

import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlLayer;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.PathLayer;
import org.ros.android.view.visualization.layer.PosePublisherLayer;
import org.ros.android.view.visualization.layer.PoseSubscriberLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import android.os.Bundle;
import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.InputEvent;
import android.view.InputDevice;
import android.util.Log;

/**
 * An app that can be used to control a remote robot. This app also demonstrates
 * how to use some of views from the rosjava android library.
 *
 * @author munjaldesai@google.com (Munjal Desai)
 * @author moesenle@google.com (Lorenz Moesenlechner)
 */
public class MainActivity extends RosActivity {

  private VisualizationView visualizationView;

  //The rosjava node which handles joystick events and publishes sensor_msgs/Joy
  private JoystickNode joystickHandler_;


  public MainActivity() {
    super("Teleop", "Teleop");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    visualizationView = (VisualizationView) findViewById(R.id.visualization);
    visualizationView.getCamera().jumpToFrame("map");
    visualizationView.onCreate(Lists.<Layer>newArrayList(new CameraControlLayer(),
            new OccupancyGridLayer("map"), new PathLayer("move_base/NavfnROS/plan"), new PathLayer(
                    "move_base_dynamic/NavfnROS/plan"), new LaserScanLayer("base_scan"),
            new PoseSubscriberLayer("simple_waypoints_server/goal_pose"), new PosePublisherLayer(
                    "simple_waypoints_server/goal_pose"), new RobotLayer("base_footprint")));

    joystickHandler_ = new JoystickNode();
  }

  /**
   * Until we get a joystick event, it is not certain what InputDevice corresponds to the joystick
   * Though there are methods for introspection, I was in a hurry and this does the job.
   */
  private void initializeJoystickHandlerIfPossibleAndNecessary(InputEvent event)
  /*************************************************************************/
  {
    if ((!joystickHandler_.isInitialized()) && ((event.getSource() &
            InputDevice.SOURCE_CLASS_BUTTON) != 0 || (event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0))
      joystickHandler_.initializeDevice(event.getDevice());
  }

  /**
   * This is an android event handler when key press/release events happen
   */
  @Override
  public boolean dispatchKeyEvent(KeyEvent event)
  /*************************************************************************/
  {
    Log.d("MainActivity", "MainActivity: dispatchKeyEvent");

    initializeJoystickHandlerIfPossibleAndNecessary(event);

    if (!joystickHandler_.isInitialized())
      return super.dispatchKeyEvent(event);

    switch(event.getAction())
    {
      case KeyEvent.ACTION_DOWN:
        if (joystickHandler_.onKeyDown(event))
          return true;
        break;
      case KeyEvent.ACTION_UP:
        if (joystickHandler_.onKeyUp(event))
          return true;
        break;
    }

    return super.dispatchKeyEvent(event);
  }

  /**
   * This is an android event handler for generic motion events. This is triggered
   * when any of the axes controls (vs simple buttons) on the joystick are used
   * where each axis has a value between -1.0 and 1.0.
   */
  @Override
  public boolean dispatchGenericMotionEvent(MotionEvent event)
  /*************************************************************************/
  {
    Log.d("MainActivity", "MainActivity: dispatchGenericMotionEvent");

    initializeJoystickHandlerIfPossibleAndNecessary(event);

    boolean isJoystickEvent = ((event.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0);
    boolean isActionMoveEvent = event.getAction() == MotionEvent.ACTION_MOVE;

    if (!isJoystickEvent || !isActionMoveEvent || !joystickHandler_.isInitialized())
      return super.dispatchGenericMotionEvent(event);

    if (joystickHandler_.onJoystickMotion(event))
      return true;

    return super.dispatchGenericMotionEvent(event);
  }


  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {

    visualizationView.init(nodeMainExecutor);
    NodeConfiguration nodeConfiguration =
            NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
                    getMasterUri());

    NodeConfiguration nodeConfiguration2 =
            NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(),
                    getMasterUri());
    nodeConfiguration2.setNodeName("ShieldTeleop/JoystickNode");

    nodeMainExecutor.execute(visualizationView, nodeConfiguration.setNodeName("android/map_view"));
    nodeMainExecutor.execute(joystickHandler_, nodeConfiguration2);
  }
}