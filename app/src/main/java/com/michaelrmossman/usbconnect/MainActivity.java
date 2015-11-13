package com.michaelrmossman.usbconnect;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    public static final String sysFolder = "/sys/devices/platform/msm_hsusb/gadget/lun0";
    public static final String devFolder = "/dev/block/vold/179:33";
    public static final String sysFile = sysFolder + "/file";
    public static final String devNull = "/dev/null";
    private static final String TAG = "MainActivity";
    private boolean mSuccess1 = false;
    private boolean mSuccess2 = false;
    private Boolean debugMode = true;

    private TextView textView1, textView2, textView4, textView5;
    private String tvText1, tvText2;
    private Boolean myADB = false;
    private Integer mResult = 0;
    private String protocolPref;
    private Boolean rootPref;
    private Switch mySwitch;
    private Toast Checking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_title));
        setSupportActionBar(toolbar);

        mySwitch = (Switch) findViewById(R.id.mySwitch);
        mySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isChecked = mySwitch.isChecked();
                if (!isChecked) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertDialogBuilder.setMessage("Have you unmounted & disconnected your phone from the PC?");
                    alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            // Turn switch OFF
                            bothCommands(false);
                        }
                    });
                    alertDialogBuilder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            /* Flick the switch back ON since we're NOT doing anything.
                               This entry is just for the sake of prosperity. Prosperity :
                               noun. the condition of prospering; having good fortune */
                            mySwitch.setChecked(true);
                        }
                    });
                    alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Flick the switch back ON since we're NOT doing anything
                            mySwitch.setChecked(true);
                        }
                    });
                    // Show alertDialog after building
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } else {
                    bothCommands(true);
                }
            }
        });

        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);
        textView4.setVisibility(View.INVISIBLE);
        textView5.setVisibility(View.INVISIBLE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, getString(R.string.warning), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        myCommand();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String PREFS = "Prefs";
        SharedPreferences mSharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        rootPref = mSharedPreferences.getBoolean("rootPref", false);
        protocolPref = mSharedPreferences.getString("protocolPref", "mtp");
    }

    public void onBackPressed() {
        // Pop fragment if the back stack is not empty
        if (mySwitch.isChecked()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setMessage("Would you like to turn off USB Mass Storage mode before exiting?");
            alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // Turn switch OFF before exiting the app
                    bothCommands(false);
                    finish();
                }
            });
            alertDialogBuilder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                }
            });
            alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Outta here ... exit & leave switch ON
                    finish();
                }
            });
            // Show alertDialog after building
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void myCommand() {
        // Try running this command WITHOUT root to check for app compatibility
        Command command = new Command(0, "if [ -d " + sysFolder + " ]; then echo "
                + getString(R.string.success) + "; else echo "
                + getString(R.string.hsusbfail) + "; fi") {

            @Override
            public void commandOutput(int id, String line) {
                try {
                    if (line.equals(getString(R.string.success))) {
                        // Success ... we can continue
                        Checking = Toast.makeText(MainActivity.this, "Checking current state ...", Toast.LENGTH_SHORT);
                        Checking.setGravity(Gravity.CENTER, Checking.getXOffset() / 2, Checking.getYOffset() / 2);
                        Checking.show();
                        myCommand0();
                    } else {
                        // No such folder, so nothing to achieve
                        textView5.setVisibility(View.VISIBLE);
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                // MUST call the super method when overriding!
                super.commandOutput(id, line);
            }

            @Override
            public void commandTerminated(int id, String reason) {
            }

            @Override
            public void commandCompleted(int id, int exitcode) {
            }
        };

        try {
            RootShell.getShell(false).add(command);
        } catch (RootDeniedException | TimeoutException | IOException exc) {
            exc.printStackTrace();
        } finally {
            if (debugMode) Log.d(TAG, "Command done!");
        }
    }

    public void myCommand0() {
        // Run this command (root optional) capturing the output
        // to show which connection mode we are CURRENTLY using
        Command command0 = new Command(1, "getprop persist.sys.usb.config") {
            @Override
            public void commandOutput(int id, String line) {
                if (line.contains("adb")) {
                    myADB = true;
                }
                if (line.contains("mass_storage")) {
                    mySwitch.setChecked(true);
                }
                // MUST call the super method when overriding!
                super.commandOutput(id, line);
            }

            @Override
            public void commandTerminated(int id, String reason) {
            }

            @Override
            public void commandCompleted(int id, int exitcode) {
                mySwitch.setEnabled(true);
            }
        };

        try {
            if (rootPref) {
                // If its preferred to check root immediately upon startup,
                // then do it here, even though is not actually needed
                RootShell.getShell(true).add(command0);
            } else {
                // Otherwise just run the command without root,
                // and worry about permissions later ...
                RootShell.getShell(false).add(command0);
            }

        } catch (RootDeniedException | TimeoutException | IOException exc) {
            exc.printStackTrace();
        } finally {
            if (debugMode) Log.d(TAG, "Command0 done!");
        }
    }

    public void bothCommands(final Boolean isOn) {
        textView4.setVisibility(View.INVISIBLE);
        mySwitch.setEnabled(false);
        myCommand1(isOn);
        myCommand2(isOn);
    }

    public void myCommand1(Boolean isOn) {
        String method;
        // This is the base of the command we're going to run as Root
        String shortCommand = "setprop persist.sys.usb.config";
        // Add parameters to the base command, as appropriate
        if (isOn) {
            // Release the MTP/PTP client connection
            final MtpClient mtpClient = new MtpClient(this.getApplicationContext());
            mtpClient.close();
            // Turn on UMS mode
            method = "mass_storage";
        } else {
            // Restore preferred communications protocol
            if (protocolPref.equals("mtp")) {
                method = "mtp";
            } else {
                method = "ptp";
            }
        }

        if (myADB) {
            // If ADB was previously set, append it to the setprop options
            method += ",adb";
        }

        String longCommand = shortCommand + " " + method + "; echo $?";
        Command command1 = new Command(2, longCommand) {
            @Override
            public void commandOutput(int id, String line) {
                try {
                    mResult = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } finally {
                    if (mResult > 0) {
                        textView1.setTextColor(Color.RED);
                        tvText1 = getString(R.string.setpropfail);
                    } else {
                        textView1.setTextColor(Color.parseColor(getString(R.string.green)));
                        tvText1 = getString(R.string.success);
                        mSuccess1 = true;
                    }
                    textView1.setText(tvText1);
                    // MUST call the super method when overriding!
                    super.commandOutput(id, line);
                }
            }

            @Override
            public void commandTerminated(int id, String reason) {
                tvText1 = getString(R.string.terminated) + " " + reason;
                textView1.setTextColor(Color.RED);
                textView1.setText(tvText1);
            }

            @Override
            public void commandCompleted(int id, int exitcode) {
                tvText1 = textView1.getText() + " " + getString(R.string.exit_code) + " " + exitcode;
                textView1.setText(tvText1);
            }
        };

        try {
            textView1.setText(null);
            textView2.setText(null);
            RootShell.getShell(true).add(command1);
        } catch (RootDeniedException | TimeoutException | IOException exc) {
            exc.printStackTrace();
            mSuccess1 = false;
        } finally {
            if (debugMode) Log.d(TAG, "Command1 done!");
        }
    }

    public void myCommand2(final Boolean isOn) {
        // Specific input for the destination file, depending on connection method chosen
        String longCommand;
        if (isOn) {
            // Turn it on
            longCommand = "if [ -d " + sysFolder + " ]; then echo " + devFolder + " > " + sysFile
                    + "; echo " + getString(R.string.success) + "; else echo " + getString(R.string.hsusbfail) + "; fi";
        } else {
            // Turn it off
            longCommand = "if [ -d " + sysFolder + " ]; then echo " + devNull + " > " + sysFile
                    + "; echo " + getString(R.string.success) + "; else echo " + getString(R.string.hsusbfail) + "; fi";
        }

        // Now do all that here
        Command command2 = new Command(3, longCommand) {
            @Override
            public void commandOutput(int id, String line) {
                // Change the text colour to GREEN if all is peachy
                if (line.equals(getString(R.string.success))) {
                    textView2.setTextColor(Color.parseColor(getString(R.string.green)));
                    mSuccess2 = true;
                }
                textView2.setText(line);
                // MUST call the super method when overriding!
                super.commandOutput(id, line);
            }

            @Override
            public void commandTerminated(int id, String reason) {
                tvText2 = getString(R.string.terminated) + " " + reason;
                textView2.setText(tvText2);
            }

            @Override
            public void commandCompleted(int id, int exitcode) {
                tvText2 = textView2.getText() + " " + getString(R.string.complete);
                textView2.setText(tvText2);
                if (mSuccess1 && mSuccess2) {
                    String tmpText;
                    if (isOn) {
                        tmpText = getString(R.string.textview4);
                        if (myADB) {
                            tmpText += getString(R.string.method_plus_adb);
                        }
                    } else {
                            tmpText = getString(R.string.textview4a)
                                    + " (" + protocolPref.toUpperCase() + ")";
                    }
                    textView4.setTextColor(Color.parseColor(getString(R.string.black)));
                    textView4.setVisibility(View.VISIBLE);
                    textView4.setText(tmpText);
                    mySwitch.setEnabled(true);
                } else {
                    textView2.setTextColor(Color.RED);
                    textView2.setText(getString(R.string.error));
                }
            }
        };

        try {
            RootShell.getShell(true).add(command2);
        } catch (RootDeniedException | TimeoutException | IOException exc) {
            exc.printStackTrace();
            mSuccess2 = false;
        } finally {
            if (debugMode) Log.d(TAG, "Command2 done!");
        }
    }
}