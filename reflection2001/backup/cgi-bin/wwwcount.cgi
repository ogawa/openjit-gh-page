#!/usr/local/bin/perl

# �����̍s���v���o�C�_�̊��ɂ��킹�ĕύX���Ă��������B
# #! �̑O�ɂ͋�s���X�y�[�X����������Ȃ��悤�ɂ��Ă��������B

#==================================================================
# ���́F wwwcount.cgi Ver3.07
# ��ҁF �Ƃق�
# �ŐV�œ����F http://wakusei.cplaza.ne.jp/twn/wwwcount.htm
# ��舵���F �t���[�\�t�g�B���p/����/�Ĕz�z�\�B�m�F���[���s�v�B
#==================================================================

#==================================================================
# �g�������F
#==================================================================
#   (����1) wwwcount.cgi
#	CGI���g�p�ł��邩�e�X�g���s���B
#
#   (����2) wwwcount.cgi?text
#	�J�E���g�A�b�v���s���A�J�E���^���e�L�X�g�ŕ\������B
#
#   (����3) wwwcount.cgi?gif
#	�J�E���g�A�b�v���s���A�J�E���^��GIF�ŕ\������B
#
#   (����4) wwwcount.cgi?hide+xxx.gif
#	�J�E���g�A�b�v���s���Axxx.gif��\������B

#==================================================================
# �����F
#==================================================================
#    ���t    Ver
# 1997.03.16 1.00 ����
# 1997.04.10 1.10 �V�O�i�������̒ǉ�
# 1997.04.28 2.00 �啝����/�e�X�g�@�\�ǉ�/GIF�A���@�\
# 1997.05.08 2.10 ���b�N�t�@�C�����c���Ă��܂��o�O���C��
# 1997.05.11 2.20 %7E �� \~ �� ~ �ɕϊ�/��URL�͕\�����Ȃ��悤�ɂ���
# 1997.07.06 2.30 10���ȏ�Â����b�N�t�@�C���������悤�ɂ���
# 1997.09.14 2.40 ���[���̃T�u�W�F�N�g�ɑO���̃A�N�Z�X������\��
# 1997.10.19 2.50 ���Ȑf�f�@�\�̋���
# 1998.02.15 2.60 �Z�L�����e�B���P�Bhide+��.gif�����ǂݎ��Ȃ��悤�ɂ����B
# 1998.09.20 2.70 SSI�Ŏg�p����ۂ� text �̈�����s�v�Ƃ���
# 1998.11.29 2.80 �����J�E���^�[�ɑΉ�����
# 1998.12.20 2.81 �J�E���^�[�j��Ώ�/SIGPIPE�Ή�
# 1999.01.17 2.91 �����J�E���^�[�̃o�O�Ή�
# 1999.01.17 2.92 ���t�t�@�C�����󂾂ƃ��|�[�g������Ȃ��o�O�Ή�
# 1999.05.30 3.00 nkf���g�p���Ȃ��悤�ɂ���
# 1999.06.06 3.01 ���b�N�t�@�C���̃p�[�~�b�V������755����0755�ɏC��
# 1999.06.27 3.02 ����A�h���X�`�F�b�N�@�\�̏����l���I�t�ɏC��
# 1999.10.03 3.03 ����ă��b�N�t�@�C���������Ă��܂����Ƃ�����o�O���C��
# 1999.10.03 3.04 name�I�v�V�����̃Z�L�����e�B�z�[���ɑΏ�
# 1999.10.03 3.05 �������L�^����悤�ύX
# 1999.12.12 3.06 name�I�v�V�����̃Z�L�����e�B�z�[���Ώ�������Ă���
# 2000.01.03 3.07 2000�N���Ή�

#==================================================================
# �J�X�^�}�C�Y�F
#==================================================================
#
# SSI�̃e�L�X�g���[�h�Ŏg�p����ꍇ�́A$mode = "text"; �Ƃ��Ă��������B
#
$mode = "text";

#
# �\��������Ⴆ��5���Ɏw�肷��ꍇ�́u$figure = 5;�v�̂悤�Ɏw�肷��B
# ������������ɂ́u$figure = 0;�v�Ǝw�肷��B
#
$figure = 6;

#
# �t�@�C�����b�N�@�\���I���ɂ���ꍇ�́u$do_file_lock = 1;�v�Ƃ���B
# �t�@�C�����b�N�@�\���I�t�ɂ���ꍇ�́u$do_file_lock = 0;�v�Ƃ���B
#
$do_file_lock = 1;

#
# ���A�h���X�`�F�b�N�@�\���I���ɂ���ꍇ�́u$do_address_check = 1;�v�Ƃ���B
# ���A�h���X�`�F�b�N�@�\���I�t�ɂ���ꍇ�́u$do_address_check = 0;�v�Ƃ���B
#
$do_address_check = 0;

#
# ���|�[�g�@�\���g���ꍇ�́u$mailto = "abc@xxx.yyy.zzz";�v�̂悤�Ɏ�����
# ���[���A�h���X��ݒ肷��B�܂��A/usr/lib/sendmail�����݂��邱�Ƃ��m�F
# ���Ă����B�ʂ̏ꏊ�ɂ���ꍇ�́A$sendmail ���K�؂ɕύX����B�ڍ׃��[��
# �̏ꍇ�́u$account_detail = 1;�v�Ƃ��A�A�N�Z�X�����݂̂����[������ꍇ
# �́u$account_detail = 0;�v�Ƃ���B
#
$mailto   = 'ogawa@is.titech.ac.jp';
$sendmail = '/usr/lib/sendmail';
$account_detail = 1;

#
# ���̃A�h���X�Ƀ}�b�`����T�C�g����� FROM �͕\�����Ȃ��B
#
$my_url = '';

#
# �J�E���^�[��
#
$count_name = "wwwcount";

#==================================================================
# �������F
#==================================================================

#
# �֘A����t�@�C����􂢏o���Ă���
# ����CGI�X�N���v�g�̃t�@�C�����̊g���q��ύX�������̂ɂȂ�B
#
$file_count  = "$count_name" . ".cnt";
$file_date   = "$count_name" . ".dat";
$file_access = "$count_name" . ".acc";
$file_lock   = "lock/$count_name" . ".loc";

#
# CGI���g�p�ł��邩�e�X�g���s���B
#
if ($ARGV[0] eq "test") {
	print "Content-type: text/html\n";
	print "\n";
	print "<HTML>\n";
	print "<HEAD><TITLE>Test</TITLE></HEAD>\n";
	print "<BODY>\n";
	print "OK. CGI�X�N���v�g�͓���\\�ł��B\n";
	print "<BR>\n";
	if ($mailto ne "") {
		if (! -f $sendmail) {
			print "<BR>NG. $sendmail �����݂��܂���B\n";
		}
	}
	if (-d $file_lock) {
		print "<BR>NG. $file_lock ���c���Ă��܂��B\n";
	}
	if (! -r $file_count) {
		print "<BR>NG. $file_count �����݂��܂���B\n";
	} elsif (! -w $file_count) {
		print "<BR>NG. $file_count ���������݉\�ł͂���܂���B\n";
	}
	if (! -r $file_date) {
		print "<BR>NG. $file_date �����݂��܂���B\n";
	} elsif (! -w $file_date) {
		print "<BR>NG. $file_date ���������݉\�ł͂���܂���B\n";
	}
	if (! -r $file_access) {
		print "<BR>NG. $file_access �����݂��܂���B\n";
	} elsif (! -w $file_access) {
		print "<BR>NG. $file_access ���������݉\�ł͂���܂���B\n";
	}
	print "</BODY>\n";
	print "</HTML>\n";
	exit(0);
}

#
# ���������߂���
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
# ���ϐ�TZ����{���Ԃɐݒ肷��
#
$ENV{'TZ'} = "JST-9";

#
# ���b�N���𓾂�
#
if ($do_file_lock) {
	foreach $i ( 1, 2, 3, 4, 5, 6 ) {
		if (mkdir("$file_lock", 0755)) {
			# ���b�N�����B���̏����ցB
			last;
		} elsif ($i == 1) {
			# 10���ȏ�Â����b�N�t�@�C���͍폜����B
			($mtime) = (stat($file_lock))[9];
			if ($mtime < time() - 600) {
				rmdir($file_lock);
			}
		} elsif ($i < 6) {
			# ���b�N���s�B1�b�҂��čăg���C�B
			sleep(1);
		} else {
			# ���x����Ă����b�N���s�B������߂�B
			exit(1);
		}
	}
}

#
# �r���ŏI�����Ă����b�N�t�@�C�����c��Ȃ��悤�ɂ���
#
sub sigexit { rmdir($file_lock); exit(0); }
$SIG{'PIPE'} = $SIG{'INT'} = $SIG{'HUP'} = $SIG{'QUIT'} = $SIG{'TERM'} = "sigexit";

#
# �J�E���^�[�t�@�C������J�E���^�[�l��ǂݏo���B
#
if (open(IN, "< $file_count")) {
	$count = <IN>;
	close(IN);
} else {
	$count = -1;
}

#
# ���t�t�@�C������ŏI�A�N�Z�X���t��ǂݏo���B
#
if (open(IN, "< $file_date")) {
	$date_log = <IN>;
	close(IN);
} else {
	$date_log = "";
}

#
# �����̓��t�𓾂�
#
($sec, $min, $hour, $mday, $mon, $year) = localtime(time());
$date_now = sprintf("%04d/%02d/%02d", 1900 + $year, $mon + 1, $mday);
$time_now = sprintf("%02d:%02d:%02d", $hour, $min, $sec);

#
# ���t���قȂ�A�܂�A�������߂ẴA�N�Z�X�ł����
#
if ($date_log ne $date_now) {

	#
	# �A�N�Z�X���O�����[���ő��M����
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
	# �A�N�Z�X���O������������
	#
	open(OUT, "> $file_access");
	close(OUT);

	#
	# �����̓��t����t���O�t�@�C���ɏ����o��
	#
	open(OUT, "> $file_date");
	print(OUT "$date_now");
	close(OUT);
}

#
# ���łɓ��A�h���X����̃A�N�Z�X������΃J�E���g�A�b�v���Ȃ�
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
# �J�E���g�A�b�v����
#
if (($count >= 0) && ($count_up == 1)) {

	#
	# �J�E���^���ЂƂC���N�������g����
	#
	$count++;

	#
	# %7E �� \~ �Ȃǂ���������
	#
	$referer = $ENV{'HTTP_REFERER'};
	$referer =~ s/%([0-9a-fA-F][0-9a-fA-F])/pack("C", hex($1))/eg;
	$reffile =~ s/\\//g;

	#
	# �A�N�Z�X���O���L�^����
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
	# �J�E���^���J�E���^�t�@�C���ɏ����߂�
	#
	if (open(OUT, "> $file_count")) {
		print(OUT "$count");
		close(OUT);
	}
}

#
# CGI�X�N���v�g�̌��ʂƂ��ăJ�E���^�[�������o��
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
# ���b�N�����J������
#
if ($do_file_lock) {
	rmdir($file_lock);
}

