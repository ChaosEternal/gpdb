#-------------------------------------------------------------------------
#
# Makefile--
#    Makefile for src/backend/postmaster
#
# IDENTIFICATION
#    $PostgreSQL: pgsql/src/backend/postmaster/Makefile,v 1.24 2008/02/19 10:30:07 petere Exp $
#
#-------------------------------------------------------------------------

subdir = src/backend/postmaster
top_builddir = ../../..
include $(top_builddir)/src/Makefile.global
override CPPFLAGS := -I$(top_srcdir)/src/backend/gp_libpq_fe $(CPPFLAGS)

OBJS = bgwriter.o autovacuum.o checkpoint.o seqserver.o pgarch.o pgstat.o \
	postmaster.o primary_mirror_mode.o primary_mirror_transition_client.o syslogger.o \
	fork_process.o perfmon.o backoff.o perfmon_segmentinfo.o \
	sendalert.o alertseverity.o

include $(top_srcdir)/src/backend/common.mk
