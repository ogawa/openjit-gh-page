#!/usr/local/bin/perl

# ↑この行をプロバイダの環境にあわせて変更してください。
# #! の前には空行もスペース文字も入れないようにしてください。

#==================================================================
# 名称： wwwcount.cgi Ver3.07
# 作者： とほほ
# 最新版入手先： http://wakusei.cplaza.ne.jp/twn/wwwcount.htm
# 取り扱い： フリーソフト。利用/改造/再配布可能。確認メール不要。
#==================================================================

#==================================================================
# 使いかた：
#==================================================================
#   (書式1) wwwcount.cgi
#	CGIが使用できるかテストを行う。
#
#   (書式2) wwwcount.cgi?text
#	カウントアップを行い、カウンタをテキストで表示する。
#
#   (書式3) wwwcount.cgi?gif
#	カウントアップを行い、カウンタをGIFで表示する。
#
#   (書式4) wwwcount.cgi?hide+xxx.gif
#	カウントアップを行い、xxx.gifを表示する。

#==================================================================
# 履歴：
#==================================================================
#    日付    Ver
# 1997.03.16 1.00 初版
# 1997.04.10 1.10 シグナル処理の追加
# 1997.04.28 2.00 大幅改造/テスト機能追加/GIF連結機能
# 1997.05.08 2.10 ロックファイルが残ってしまうバグを修正
# 1997.05.11 2.20 %7E や \~ を ~ に変換/自URLは表示しないようにした
# 1997.07.06 2.30 10分以上古いロックファイルを消すようにした
# 1997.09.14 2.40 メールのサブジェクトに前日のアクセス件数を表示
# 1997.10.19 2.50 自己診断機能の強化
# 1998.02.15 2.60 セキュリティ改善。hide+で.gifしか読み取れないようにした。
# 1998.09.20 2.70 SSIで使用する際に text の引数を不要とした
# 1998.11.29 2.80 複数カウンターに対応した
# 1998.12.20 2.81 カウンター破壊対処/SIGPIPE対応
# 1999.01.17 2.91 複数カウンターのバグ対応
# 1999.01.17 2.92 日付ファイルが空だとレポートが送れないバグ対応
# 1999.05.30 3.00 nkfを使用しないようにした
# 1999.06.06 3.01 ロックファイルのパーミッションを755から0755に修正
# 1999.06.27 3.02 同一アドレスチェック機能の初期値をオフに修正
# 1999.10.03 3.03 誤ってロックファイルを消してしまうことがあるバグを修正
# 1999.10.03 3.04 nameオプションのセキュリティホールに対処
# 1999.10.03 3.05 時刻も記録するよう変更
# 1999.12.12 3.06 nameオプションのセキュリティホール対処が誤っていた
# 2000.01.03 3.07 2000年問題対応

#==================================================================
# カスタマイズ：
#==================================================================
#
# SSIのテキストモードで使用する場合は、$mode = "text"; としてください。
#
$mode = "text";

#
# 表示桁数を例えば5桁に指定する場合は「$figure = 5;」のように指定する。
# 自動調整するには「$figure = 0;」と指定する。
#
$figure = 6;

#
# ファイルロック機能をオンにする場合は「$do_file_lock = 1;」とする。
# ファイルロック機能をオフにする場合は「$do_file_lock = 0;」とする。
#
$do_file_lock = 1;

#
# 同アドレスチェック機能をオンにする場合は「$do_address_check = 1;」とする。
# 同アドレスチェック機能をオフにする場合は「$do_address_check = 0;」とする。
#
$do_address_check = 0;

#
# レポート機能を使う場合は「$mailto = "abc@xxx.yyy.zzz";」のように自分の
# メールアドレスを設定する。また、/usr/lib/sendmailが存在することを確認
# しておく。別の場所にある場合は、$sendmail も適切に変更する。詳細メール
# の場合は「$account_detail = 1;」とし、アクセス件数のみをメールする場合
# は「$account_detail = 0;」とする。
#
$mailto   = 'ogawa@is.titech.ac.jp';
$sendmail = '/usr/lib/sendmail';
$account_detail = 1;

#
# このアドレスにマッチするサイトからの FROM は表示しない。
#
$my_url = '';

#
# カウンター名
#
$count_name = "wwwcount";

#==================================================================
# 処理部：
#==================================================================

#
# 関連するファイルを洗い出しておく
# このCGIスクリプトのファイル名の拡張子を変更したものになる。
#
$file_count  = "$count_name" . ".cnt";
$file_date   = "$count_name" . ".dat";
$file_access = "$count_name" . ".acc";
$file_lock   = "lock/$count_name" . ".loc";

#
# CGIが使用できるかテストを行う。
#
if ($ARGV[0] eq "test") {
	print "Content-type: text/html\n";
	print "\n";
	print "<HTML>\n";
	print "<HEAD><TITLE>Test</TITLE></HEAD>\n";
	print "<BODY>\n";
	print "OK. CGIスクリプトは動作可能\です。\n";
	print "<BR>\n";
	if ($mailto ne "") {
		if (! -f $sendmail) {
			print "<BR>NG. $sendmail が存在しません。\n";
		}
	}
	if (-d $file_lock) {
		print "<BR>NG. $file_lock が残っています。\n";
	}
	if (! -r $file_count) {
		print "<BR>NG. $file_count が存在しません。\n";
	} elsif (! -w $file_count) {
		print "<BR>NG. $file_count が書き込み可能ではありません。\n";
	}
	if (! -r $file_date) {
		print "<BR>NG. $file_date が存在しません。\n";
	} elsif (! -w $file_date) {
		print "<BR>NG. $file_date が書き込み可能ではありません。\n";
	}
	if (! -r $file_access) {
		print "<BR>NG. $file_access が存在しません。\n";
	} elsif (! -w $file_access) {
		print "<BR>NG. $file_access が書き込み可能ではありません。\n";
	}
	print "</BODY>\n";
	print "</HTML>\n";
	exit(0);
}

#
# 引数を解釈する
#
for ($i = 0; $i <= $#ARGV; $i++) {
	if ($ARGV[$i] eq "text") {
		$mode = "text";
	} elsif ($ARGV[$i] eq "gif") {
		$mode = "gif";
	} elsif ($ARGV[$i] eq "hide") {
		$mode = "hide";
		$giffile = $ARGV[++$i];
		if (!($giffile =~ /\.gif$/i)) {
			exit(1);
		}
		if ($giffile =~ /[<>|&]/) {
			exit(1);
		}
	} elsif ($ARGV[$i] eq "name") {
		$count_name = $ARGV[++$i];
		if ($count_name !~ /^[a-zA-Z0-9]+$/) {
			exit(1);
		}
		$file_count  = "$count_name" . ".cnt";
		$file_date   = "$count_name" . ".dat";
		$file_access = "$count_name" . ".acc";
	} elsif ($ARGV[$i] eq "ref") {
		$reffile = $ARGV[++$i];
	}
}

#
# 環境変数TZを日本時間に設定する
#
$ENV{'TZ'} = "JST-9";

#
# ロック権を得る
#
if ($do_file_lock) {
	foreach $i ( 1, 2, 3, 4, 5, 6 ) {
		if (mkdir("$file_lock", 0755)) {
			# ロック成功。次の処理へ。
			last;
		} elsif ($i == 1) {
			# 10分以上古いロックファイルは削除する。
			($mtime) = (stat($file_lock))[9];
			if ($mtime < time() - 600) {
				rmdir($file_lock);
			}
		} elsif ($i < 6) {
			# ロック失敗。1秒待って再トライ。
			sleep(1);
		} else {
			# 何度やってもロック失敗。あきらめる。
			exit(1);
		}
	}
}

#
# 途中で終了してもロックファイルが残らないようにする
#
sub sigexit { rmdir($file_lock); exit(0); }
$SIG{'PIPE'} = $SIG{'INT'} = $SIG{'HUP'} = $SIG{'QUIT'} = $SIG{'TERM'} = "sigexit";

#
# カウンターファイルからカウンター値を読み出す。
#
if (open(IN, "< $file_count")) {
	$count = <IN>;
	close(IN);
} else {
	$count = -1;
}

#
# 日付ファイルから最終アクセス日付を読み出す。
#
if (open(IN, "< $file_date")) {
	$date_log = <IN>;
	close(IN);
} else {
	$date_log = "";
}

#
# 今日の日付を得る
#
($sec, $min, $hour, $mday, $mon, $year) = localtime(time());
$date_now = sprintf("%04d/%02d/%02d", 1900 + $year, $mon + 1, $mday);
$time_now = sprintf("%02d:%02d:%02d", $hour, $min, $sec);

#
# 日付が異なる、つまり、今日初めてのアクセスであれば
#
if ($date_log ne $date_now) {

	#
	# アクセスログをメールで送信する
	#
	if ($mailto ne "") {
		$tmp_count = 0;
		open(IN, "< $file_access");
		while (<IN>) {
			if (/^COUNT/) {
				$tmp_count++;
			}
		}
		close(IN);
		$msg = "";
		$msg .= "To: $mailto\n";
		$msg .= "From: $count_name\n";
		$msg .= "Subject: ACCESS $date_log $tmp_count\n";
		$msg .= "\n";
		if ($account_detail) {
			open(IN, "< $file_access");
			while (<IN>) {
				$msg .= $_;
			}
			close(IN);
		} else {
			$msg .= "Access = $tmp_count\n";
		}
		open(OUT, "| $sendmail $mailto");
		print OUT $msg;
		close(OUT);
	}

	#
	# アクセスログを初期化する
	#
	open(OUT, "> $file_access");
	close(OUT);

	#
	# 今日の日付を日付ログファイルに書き出す
	#
	open(OUT, "> $file_date");
	print(OUT "$date_now");
	close(OUT);
}

#
# すでに同アドレスからのアクセスがあればカウントアップしない
#
$count_up = 1;
if ($do_address_check) {
	open(IN, "$file_access");
	while (<IN>) {
		if ($_ eq "ADDR  = [ $ENV{'REMOTE_ADDR'} ]\n") {
			$count_up = 0;
			last;
		}
	}
	close(IN);
}

#
# カウントアップ処理
#
if (($count >= 0) && ($count_up == 1)) {

	#
	# カウンタをひとつインクリメントする
	#
	$count++;

	#
	# %7E や \~ などを処理する
	#
	$referer = $ENV{'HTTP_REFERER'};
	$referer =~ s/%([0-9a-fA-F][0-9a-fA-F])/pack("C", hex($1))/eg;
	$reffile =~ s/\\//g;

	#
	# アクセスログを記録する
	#
	open(OUT, ">> $file_access");
	print(OUT "COUNT = [ $count ]\n");
	print(OUT "TIME  = [ $time_now ]\n");
	print(OUT "ADDR  = [ $ENV{'REMOTE_ADDR'} ]\n");
	if ($ENV{'REMOTE_HOST'} ne $ENV{'REMOTE_ADDR'}) {
		print(OUT "HOST  = [ $ENV{'REMOTE_HOST'} ]\n");
	}
	print(OUT "AGENT = [ $ENV{'HTTP_USER_AGENT'} ]\n");
	# print(OUT "REFER = [ $referer ]\n");
	if ($reffile && (!$my_url || ($reffile !~ /$my_url/))) {
		print(OUT "FROM  = [ $reffile ]\n");
	}
	print(OUT "\n");
	close(OUT);

	#
	# カウンタをカウンタファイルに書き戻す
	#
	if (open(OUT, "> $file_count")) {
		print(OUT "$count");
		close(OUT);
	}
}

#
# CGIスクリプトの結果としてカウンターを書き出す
#
if ($count == -1) {
	$count = 0;
}
if ($figure != 0) {
	$cntstr = sprintf(sprintf("%%0%dld", $figure), $count);
} else {
	$cntstr = sprintf("%ld", $count);
}
if ($mode eq "text") {
	printf("Content-type: text/html\n");
	printf("\n");
	printf("$cntstr\n");
} elsif ($mode eq "gif") {
	printf("Content-type: image/gif\n");
	printf("\n");
	for ($i = 0; $i < length($cntstr); $i++) {
		$n = substr($cntstr, $i, 1);
		push(@files, "$n.gif");
	}
	require "gifcat.pl";
	binmode(STDOUT);
	print &gifcat'gifcat(@files);
} elsif ($mode eq "hide") {
	printf("Content-type: image/gif\n");
	printf("\n");
	$size = -s $giffile;
	open(IN, $giffile);
	binmode(IN);
	binmode(STDOUT);
	read(IN, $buf, $size);
	print $buf;
	close(IN);
}

#
# ロック権を開放する
#
if ($do_file_lock) {
	rmdir($file_lock);
}

