package cn.ctkqiang.huaxiahongke.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Objects;

import cn.ctkqiang.huaxiahongke.R;
import cn.ctkqiang.huaxiahongke.constants.Constants;

@SuppressWarnings("NonAsciiCharacters")
public class ShellActivity extends AppCompatActivity
{
    private static final String 标签 = Constants.TAG_NAME;
    private static final int 请求码_存储权限 = 123;

    private TextView terminalOutput;
    private EditText commandInput;
    private ScrollView scrollView;

    private String currentDirectory = "/";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_shell_acitivity);

        Objects.requireNonNull(this.getSupportActionBar()).hide(); // 隐藏ActionBar

        this.terminalOutput = (TextView) this.findViewById(R.id.terminalOutput);
        this.commandInput = (EditText) this.findViewById(R.id.commandInput);
        this.scrollView = (ScrollView) this.findViewById(R.id.scrollView);

        this.terminalOutput.setTypeface(Typeface.MONOSPACE);

        if (!this.检查存储权限())
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    请求码_存储权限);
        }

        // 显示设备信息
        this.显示设备信息();

        this.commandInput.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN))
                {

                    String 命令文本 = ShellActivity.this.commandInput.getText().toString().trim();
                    if (!命令文本.isEmpty())
                    {
                        ShellActivity.this.追加输出("$ " + 命令文本);
                        ShellActivity.this.执行Shell命令(命令文本);
                        ShellActivity.this.commandInput.setText("");
                    }
                    return true;
                }
                return false;
            }
        });
    }

    // 在ShellActivity类中添加执行curl命令的方法
    private void 执行Curl命令(String 命令)
    {
        try
        {
            // 解析curl命令参数
            String[] 命令部分 = 命令.split(" ");
            if (命令部分.length < 2)
            {
                this.追加输出("使用方法: curl [选项] URL");
                this.追加输出("示例: curl https://example.com");
                return;
            }

            // 提取URL (最后一个参数通常是URL)
            String url = 命令部分[命令部分.length - 1];

            // 创建StringBuilder来构建curl命令
            StringBuilder curlCommand = new StringBuilder("curl");

            // 处理选项
            boolean hasOptions = false;
            for (int i = 1; i < 命令部分.length - 1; i++)
            {
                curlCommand.append(" ").append(命令部分[i]);
                hasOptions = true;
            }

            // 添加URL
            curlCommand.append(" ").append(url);

            // 执行curl命令
            Process process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", curlCommand.toString()});

            // 读取输出
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            final StringBuilder result = new StringBuilder();

            String line;
            while ((line = outputReader.readLine()) != null)
            {
                result.append(line).append("\n");
            }

            while ((line = errorReader.readLine()) != null)
            {
                result.append(line).append("\n");
            }

            process.waitFor();

            // 显示结果
            this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (result.toString().trim().isEmpty())
                    {
                        ShellActivity.this.追加输出("[无输出]");
                    } else
                    {
                        ShellActivity.this.追加输出(result.toString());
                    }
                }
            });

        } catch (final Exception e)
        {
            this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    ShellActivity.this.追加输出("Curl 执行失败: " + e.getMessage());
                }
            });
        }
    }

    private boolean 检查存储权限()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void 执行Shell命令(final String 命令字符串)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {

                    if (命令字符串.equals("help") || 命令字符串.equals("--help"))
                    {
                        ShellActivity.this.追加输出("可用命令:\n" +
                                "ls [目录] - 列出目录内容\n" +
                                "pwd - 显示当前目录\n" +
                                "cd [目录] - 切换目录\n" +
                                "cat [文件] - 查看文件内容\n" +
                                "mkdir [目录] - 创建目录\n" +
                                "rm [文件或目录] - 删除文件或目录\n" +
                                "python3 [命令] - 执行 Python 代码\n" +
                                "curl [URL] - 执行 curl 请求");
                        return;
                    }

                    // 只允许执行一般的 shell 命令，禁止执行需要 root 权限的命令
                    if (是否危险路径(命令字符串))
                    {
                        ShellActivity.this.追加输出("🚫 无权限访问此路径，尝试使用 /sdcard 或 /storage/emulated/0/");
                        return;
                    }

                    // 支持基本的 Linux 命令
                    if (命令字符串.startsWith("ls"))
                    {
                        ShellActivity.this.列出文件(命令字符串);
                    } else if (命令字符串.startsWith("pwd"))
                    {
                        ShellActivity.this.打印当前目录();
                    } else if (命令字符串.startsWith("cd"))
                    {
                        ShellActivity.this.切换目录(命令字符串);
                    } else if (命令字符串.startsWith("curl"))
                    {
                        ShellActivity.this.执行Curl命令(命令字符串); // 新增curl命令处理
                    } else if (命令字符串.startsWith("cat"))
                    {
                        ShellActivity.this.查看文件内容(命令字符串);
                    } else if (命令字符串.startsWith("mkdir"))
                    {
                        ShellActivity.this.创建目录(命令字符串);
                    } else if (命令字符串.startsWith("rm"))
                    {
                        ShellActivity.this.删除文件或目录(命令字符串);
                    } else if (命令字符串.startsWith("python3") || 命令字符串.startsWith("pip"))
                    {
                        ShellActivity.this.执行Python命令(命令字符串);
                    } else
                    {
                        // 其他普通命令
                        Process 进程 = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", 命令字符串});

                        BufferedReader 输出读取器 = new BufferedReader(new InputStreamReader(进程.getInputStream()));
                        BufferedReader 错误读取器 = new BufferedReader(new InputStreamReader(进程.getErrorStream()));

                        final StringBuilder 输出结果 = new StringBuilder();

                        String 行;
                        while ((行 = 输出读取器.readLine()) != null)
                        {
                            输出结果.append(行).append("\n");
                        }
                        while ((行 = 错误读取器.readLine()) != null)
                        {
                            输出结果.append(行).append("\n");
                        }

                        进程.waitFor();

                        ShellActivity.this.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if (输出结果.toString().trim().isEmpty())
                                {
                                    ShellActivity.this.追加输出("[无输出]");
                                } else
                                {
                                    ShellActivity.this.追加输出(输出结果.toString());
                                }
                            }
                        });
                    }

                } catch (final Exception 异常)
                {
                    ShellActivity.this.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ShellActivity.this.追加输出("命令执行失败: " + 异常.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void 列出文件(String 命令)
    {
        try
        {
            String listDir = 命令.equals("ls") ? this.currentDirectory : 命令.split(" ")[1];
            File dir = new File(listDir);
            if (!dir.exists() || !dir.isDirectory())
            {
                this.追加输出("目录不存在");
                return;
            }
            String[] files = dir.list();
            if (files == null || files.length == 0)
            {
                this.追加输出("目录为空");
                return;
            }
            for (String file : files)
            {
                this.追加输出(file);
            }
        } catch (Exception e)
        {
            this.追加输出("列出文件错误: " + e.getMessage());
        }
    }

    private void 打印当前目录()
    {
        this.追加输出(this.currentDirectory);
    }

    private void 切换目录(String 命令)
    {
        String[] parts = 命令.split(" ");
        if (parts.length < 2)
        {
            this.追加输出("请输入目录路径");
            return;
        }
        File newDir = new File(parts[1]);
        if (newDir.exists() && newDir.isDirectory())
        {
            this.currentDirectory = newDir.getAbsolutePath();
            this.追加输出("切换到: " + this.currentDirectory);
        } else
        {
            this.追加输出("目录不存在");
        }
    }

    private void 查看文件内容(String 命令)
    {
        String[] parts = 命令.split(" ");
        if (parts.length < 2)
        {
            this.追加输出("请输入文件路径");
            return;
        }
        try
        {
            File file = new File(parts[1]);
            if (!file.exists() || file.isDirectory())
            {
                this.追加输出("文件不存在或是目录");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(file.toURI().toURL().openStream()));
            String line;
            while ((line = reader.readLine()) != null)
            {
                this.追加输出(line);
            }
            reader.close();
        } catch (Exception e)
        {
            this.追加输出("读取文件错误: " + e.getMessage());
        }
    }

    private void 创建目录(String 命令)
    {
        String[] parts = 命令.split(" ");
        if (parts.length < 2)
        {
            this.追加输出("请输入目录路径");
            return;
        }
        File newDir = new File(parts[1]);
        if (newDir.exists())
        {
            this.追加输出("目录已存在");
        } else if (newDir.mkdirs())
        {
            this.追加输出("目录创建成功: " + parts[1]);
        } else
        {
            this.追加输出("目录创建失败");
        }
    }

    private void 删除文件或目录(String 命令)
    {
        String[] parts = 命令.split(" ");
        if (parts.length < 2)
        {
            this.追加输出("请输入文件或目录路径");
            return;
        }
        File file = new File(parts[1]);
        if (file.exists())
        {
            if (file.isDirectory())
            {
                if (deleteDirectory(file))
                {
                    this.追加输出("目录删除成功: " + parts[1]);
                } else
                {
                    this.追加输出("目录删除失败");
                }
            } else if (file.delete())
            {
                this.追加输出("文件删除成功: " + parts[1]);
            } else
            {
                this.追加输出("文件删除失败");
            }
        } else
        {
            this.追加输出("文件或目录不存在");
        }
    }

    private boolean deleteDirectory(File directory)
    {
        if (directory.isDirectory())
        {
            String[] files = directory.list();

            if (files != null)
            {
                for (String file : files)
                {
                    File currentFile = new File(directory, file);
                    if (!deleteDirectory(currentFile))
                    {
                        return false;
                    }
                }
            }
        }
        return directory.delete();
    }

    private void 执行Python命令(String 命令)
    {
        try
        {
            // 检查 Python 是否已安装
            Process checkPythonProcess = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", "python3 --version"});
            BufferedReader checkPythonReader = new BufferedReader(new InputStreamReader(checkPythonProcess.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = checkPythonReader.readLine()) != null)
            {
                output.append(line).append("\n");
            }

            checkPythonProcess.waitFor();

            // 如果没有输出，表示没有安装 Python
            if (output.toString().trim().isEmpty())
            {
                this.追加输出("🚫 Python 未安装。请安装 Python 后重试。");
                // 可选：可以提示用户安装 Python，提供安装链接或说明
                this.追加输出("安装 Python: https://play.google.com/store/apps/details?id=org.pythons.android");

                return;
            }

            // 如果 Python 已安装，则执行 Python 命令
            Process process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", 命令});

            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            final StringBuilder result = new StringBuilder();

            while ((line = outputReader.readLine()) != null)
            {
                result.append(line).append("\n");
            }

            while ((line = errorReader.readLine()) != null)
            {
                result.append(line).append("\n");
            }

            process.waitFor();

            // 输出 Python 命令执行的结果
            this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (result.toString().trim().isEmpty())
                    {
                        ShellActivity.this.追加输出("[无输出]");
                    } else
                    {
                        ShellActivity.this.追加输出(result.toString());
                    }
                }
            });

        } catch (final Exception e)
        {
            this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    ShellActivity.this.追加输出("Python 执行失败: " + e.getMessage());
                }
            });
        }
    }


    private boolean 是否危险路径(String 命令)
    {
        return 命令.matches(".*\\b(/data|/system|/proc|/vendor|/sbin|/mnt|/acct)\\b.*");
    }

    private void 追加输出(final String 信息)
    {
        this.terminalOutput.append(信息 + "\n");
        this.scrollView.post(new Runnable()
        {
            @Override
            public void run()
            {
                ShellActivity.this.scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void 显示设备信息()
    {
        int maxLabelLength = 0;

        String asciiArt =
                "          \\ \\/ /\n" +
                        "        `._/\\.'\n" +
                        "         (o^.^)\n" +
                        "          | V |\n" +
                        "         /  |  \\\n" +
                        "        /   |   \\\n" +
                        "       /    |    \\\n" +
                        "      /     |     \\\n" +
                        "    _/______|______\\_\n" +
                        "   /   (o)          (o)   \\\n" +
                        "  /                        \\\n" +
                        " ============================\n" +
                        "     华夏红客工具           \n" +
                        " ============================\n";

        StringBuilder deviceInfo = new StringBuilder();

        deviceInfo.append(asciiArt);
        deviceInfo.append("设备信息:\n");

        String[] labels = {"设备型号", "操作系统版本", "SDK版本", "硬件", "设备厂商", "品牌", "产品"};

        String[] values = {
                Build.MODEL,
                Build.VERSION.RELEASE,
                String.valueOf(Build.VERSION.SDK_INT),
                Build.HARDWARE,
                Build.MANUFACTURER,
                Build.BRAND,
                Build.PRODUCT
        };


        for (String label : labels)
        {
            maxLabelLength = Math.max(maxLabelLength, label.length());
        }

        for (int i = 0; i < labels.length; i++)
        {
            String label = labels[i];
            String value = values[i];

            deviceInfo.append(String.format("%-" + maxLabelLength + "s : %s\n", label, value));
        }

        this.追加输出(deviceInfo.toString());
    }

}
