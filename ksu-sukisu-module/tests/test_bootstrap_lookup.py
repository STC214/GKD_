"""Host-only regression of the actual installer lookup loop; requires bash and awk, no ADB."""
import argparse,json,pathlib,shutil,subprocess,tempfile
p=argparse.ArgumentParser();p.add_argument('--report',type=pathlib.Path);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];source=(root/'module/customize.sh').read_text(encoding='utf8')
start=source.index('    A11Y_SERVICE=\n');end=source.index('    mkdir -p "$APP_FILES"',start);lookup=source[start:end]
bash=shutil.which('bash');assert bash,'bash required'
component='li.songe.gkd/com.google.android.accessibility.selecttospeak.SelectToSpeakService';records=[]
for case,mode,expected_calls,success in [('plain','plain',1,True),('indented-platform-output','indented',1,True),('tab-indented','tab',1,True),('metadata-decoy','decoy',1,True),('settling-two-empty-results','settling',3,True),('missing-service','missing',5,False),('wrong-package','wrong',5,False)]:
 with tempfile.TemporaryDirectory(prefix='gkd-lookup-') as d:
  header='''set -eu
GKD_PACKAGE=li.songe.gkd
abort() { echo "$*" >&2; exit 73; }
sleep() { echo sleep >> sleeps; }
cmd() {
 n=0; [ ! -f calls ] || n=$(cat calls)
 n=$((n+1)); echo "$n" > calls
 case "$MODE" in
 plain) printf '%s\\n' "$COMPONENT";;
 indented) printf '%s\\n' '1 services found:' '  Service #0:' '    priority=0 preferredOrder=0' "    $COMPONENT";;
 tab) printf '\\t%s\\n' "$COMPONENT";;
 decoy) printf '%s\\n' 'metadata li.songe.gkd/not_a_service' 'li.songe.gkd/decoy extra-field' "    $COMPONENT";;
 settling) [ "$n" -lt 3 ] || printf '%s\\n' "$COMPONENT"; return 0;;
 missing) echo 'No services found';;
 wrong) echo 'li.songe.gkd.evil/.Service';;
 esac
}
'''
  import os
  env=dict(os.environ,MODE=mode,COMPONENT=component)
  result=subprocess.run([bash,'-c',header+lookup+'\nprintf "%s\\n" "$A11Y_SERVICE"\n'],cwd=d,env=env,capture_output=True)
  calls=int((pathlib.Path(d)/'calls').read_text());sleeps=(pathlib.Path(d)/'sleeps');sleep_count=len(sleeps.read_text().splitlines()) if sleeps.exists() else 0
  row=dict(case=case,exit=result.returncode,stdout=result.stdout.decode('utf8','replace'),stderr=result.stderr.decode('utf8','replace'),calls=calls,sleeps=sleep_count);records.append(row)
  assert calls==expected_calls and (result.returncode==0)==success,row
  assert sleep_count==(calls-1),row
  if success:assert row['stdout'].strip()==component,row
  else:assert result.returncode==73 and 'lookup failed' in row['stderr'],row
  print('PASS',case)
report=dict(passed=True,cases=len(records),adbCalls=0,records=records)
if a.report:a.report.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
