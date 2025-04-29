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
                                "ssh [用户名@]主机名[:端口] - 连接到SSH服务器\n" +
                                "ssh [选项] [用户名@]主机名 - 使用选项连接SSH服务器" +
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
                    } else if (命令字符串.startsWith("ssh"))
                    {
                        ShellActivity.this.执行SSH命令(命令字符串);
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

    // 在ShellActivity类中添加执行SSH命令的方法
    private void 执行SSH命令(String 命令字符串)
    {
        try
        {
            // 解析SSH命令参数
            String[] 命令部分 = 命令字符串.split(" ");

            // 检查是否为帮助命令
            if (命令部分.length == 2 && (命令部分[1].equals("--help") || 命令部分[1].equals("-h")))
            {
                显示SSH使用提示();
                return;
            }

            if (命令部分.length < 2)
            {
                this.追加输出("使用方法: ssh [选项] [用户名@]主机名[:端口]");
                this.追加输出("获取详细帮助请使用: ssh --help");
                return;
            }

            // 验证是否安装了SSH客户端
            if (!检查SSH客户端())
            {
                this.追加输出("🚫 SSH客户端未安装。请先安装SSH客户端应用。");
                this.追加输出("推荐安装: JuiceSSH, Termux或ConnectBot等应用");
                this.追加输出("或者执行: apt install openssh 安装SSH客户端");
                return;
            }

            // 获取主机信息
            String hostInfo = 命令部分[命令部分.length - 1];

            // 显示SSH连接横幅
            显示SSH连接横幅(hostInfo);

            // 显示连接状态
            显示彩色提示("info", "正在连接到SSH服务器: " + hostInfo);

            // 构建完整的SSH命令
            StringBuilder fullCommand = new StringBuilder("ssh");
            for (int i = 1; i < 命令部分.length; i++)
            {
                fullCommand.append(" ").append(命令部分[i]);
            }

            // 执行SSH命令
            Process sshProcess = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", fullCommand.toString()});

            // 读取SSH输出
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(sshProcess.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(sshProcess.getErrorStream()));

            // 创建输出结果构建器
            final StringBuilder outputResult = new StringBuilder();

            // 读取标准输出
            String line;
            while ((line = outputReader.readLine()) != null)
            {
                final String currentLine = line;
                this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ShellActivity.this.追加输出(currentLine);
                    }
                });
            }

            // 读取错误输出
            while ((line = errorReader.readLine()) != null)
            {
                final String currentLine = line;
                this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ShellActivity.this.追加输出("错误: " + currentLine);
                    }
                });
            }

            // 等待进程完成
            int exitCode = sshProcess.waitFor();

            // 显示SSH连接结束横幅
            显示SSH连接结束横幅(exitCode);

            // 根据退出代码显示不同的提示
            if (exitCode == 0)
            {
                显示彩色提示("success", "SSH会话正常结束");
            } else
            {
                显示彩色提示("warning", "SSH会话异常结束，退出代码: " + exitCode);
            }

        } catch (final Exception e)
        {
            this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    ShellActivity.this.追加输出("SSH执行失败: " + e.getMessage());
                }
            });
        }
    }


    /**
     * 显示彩色提示信息
     *
     * @param 提示类型 提示类型(info, success, warning, error)
     * @param 消息   提示消息
     */
    private void 显示彩色提示(String 提示类型, String 消息)
    {
        String prefix;
        switch (提示类型)
        {
            case "success":
                prefix = "✅ ";
                break;
            case "warning":
                prefix = "⚠️ ";
                break;
            case "error":
                prefix = "❌ ";
                break;
            case "info":
            default:
                prefix = "ℹ️ ";
                break;
        }

        this.追加输出(prefix + 消息);
    }

    /**
     * 显示SSH连接横幅
     *
     * @param 主机信息 SSH主机信息
     */
    private void 显示SSH连接横幅(String 主机信息)
    {
        StringBuilder banner = new StringBuilder();
        banner.append("\n");
        banner.append("======================================\n");
        banner.append("      SSH 连接: ").append(主机信息).append("\n");
        banner.append("      ").append(new java.util.Date().toString()).append("\n");
        banner.append("======================================\n");

        this.追加输出(banner.toString());
    }

    /**
     * 显示SSH连接结束横幅
     *
     * @param 退出代码 SSH会话退出代码
     */
    private void 显示SSH连接结束横幅(int 退出代码)
    {
        StringBuilder banner = new StringBuilder();
        banner.append("\n");
        banner.append("======================================\n");
        banner.append("      SSH 会话已结束                  \n");
        banner.append("      退出代码: ").append(退出代码).append("\n");
        banner.append("======================================\n");

        this.追加输出(banner.toString());
    }

    /**
     * 显示SSH连接状态信息
     *
     * @param 主机信息 SSH主机信息
     * @param 状态   连接状态信息
     */
    private void 显示SSH连接状态(final String 主机信息, final String 状态)
    {
        this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ShellActivity.this.追加输出("SSH状态 [" + 主机信息 + "]: " + 状态);
            }
        });
    }

    /**
     * 检查是否存在SSH客户端
     *
     * @return 如果存在返回true，否则返回false
     */
    private boolean 检查SSH客户端()
    {
        try
        {
            Process process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", "which ssh"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String path = reader.readLine();
            process.waitFor();

            return path != null && !path.isEmpty();
        } catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 显示SSH命令使用提示
     */
    private void 显示SSH使用提示()
    {
        StringBuilder help = new StringBuilder();
        help.append("SSH命令使用指南:\n");
        help.append("  基本用法: ssh 用户名@主机名\n");
        help.append("  指定端口: ssh -p 端口号 用户名@主机名\n");
        help.append("  密钥认证: ssh -i 密钥文件 用户名@主机名\n");
        help.append("  常用选项:\n");
        help.append("    -p 端口: 指定连接端口\n");
        help.append("    -i 文件: 指定身份文件(私钥)\n");
        help.append("    -v: 显示详细连接信息\n");
        help.append("    -4/-6: 强制使用IPv4/IPv6\n");
        help.append("  示例:\n");
        help.append("    ssh user@192.168.1.100\n");
        help.append("    ssh -p 2222 admin@example.com\n");

        this.追加输出(help.toString());
    }

}
