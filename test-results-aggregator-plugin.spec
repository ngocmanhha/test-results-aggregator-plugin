%global     plugin_name test-results-aggregator-plugin
%global     debug_package %{nil}
Name:       jenkins-in-house-plugins-%{plugin_name}
Version:    1.1.11
Release:    1%{?dist}
Summary:    A jenkins in-house plugins %{plugin_name}.hpi
Obsoletes:  jenkins-upstream-plugins-%{plugin_name} <= %{version}
Requires:   jenkins
Group:      Development/Libraries
License:    BSD
URL:        https://github.com/gooddata/%{plugin_name}
Source0:    %{name}.tar.gz

BuildRequires: java
BuildRequires: maven

%description
Packaged jenkins-in-house-plugin-%{plugin_name} %{plugin_name}.hpi file

%prep
%setup -n %{name} -c 

%build
mvn versions:set -DnewVersion=%{version}
mvn versions:commit
mvn package --batch-mode -Dmaven.test.skip=true
# temporary skip test for now until we done refactor unit test for this rpm

%install
%{__mkdir_p} %{buildroot}%{_sharedstatedir}/juseppe
%{__cp} target/%{plugin_name}.hpi %{buildroot}%{_sharedstatedir}/juseppe/

%files
%defattr(-,root,root,-)
%dir %{_sharedstatedir}/juseppe
%{_sharedstatedir}/juseppe/%{plugin_name}.hpi

%changelog
* Wed Nov 09 2022 +0700 Manh Ha <manh.ha@gooddata.com> - 1.1.11-1
- CONFIG: SETI-7554 build rpm package
- Support collect test results based on build number for pipeline type
