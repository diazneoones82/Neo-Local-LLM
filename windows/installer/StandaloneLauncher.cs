using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Windows.Forms;

namespace NEOLocalLMInstaller
{
    internal static class StandaloneLauncher
    {
        private const string AppName = "Neo Local LLM";
        private const string Version = "1.0.17";

        [STAThread]
        private static int Main()
        {
            try
            {
                string installDir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "Neo Local LLM",
                    "desktop-app",
                    Version);
                string launcher = Path.Combine(installDir, AppName + ".exe");

                if (!File.Exists(launcher))
                {
                    Directory.CreateDirectory(installDir);
                    string marker = Path.Combine(installDir, ".extracting");
                    File.WriteAllText(marker, DateTime.UtcNow.ToString("O"));
                    ExtractAppImage(installDir);
                    File.Delete(marker);
                }

                Process.Start(new ProcessStartInfo
                {
                    FileName = launcher,
                    WorkingDirectory = installDir,
                    UseShellExecute = true
                });
                return 0;
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString(), AppName, MessageBoxButtons.OK, MessageBoxIcon.Error);
                return 1;
            }
        }

        private static void ExtractAppImage(string installDir)
        {
            Assembly assembly = Assembly.GetExecutingAssembly();
            using (Stream zip = assembly.GetManifestResourceStream("app.zip"))
            {
                if (zip == null)
                {
                    throw new InvalidOperationException("Embedded app package is missing.");
                }

                string tempZip = Path.Combine(Path.GetTempPath(), "neo-local-lm-desktop.zip");
                using (FileStream file = File.Create(tempZip))
                {
                    zip.CopyTo(file);
                }

                ZipFile.ExtractToDirectory(tempZip, installDir);
                File.Delete(tempZip);
            }
        }
    }
}
