
Pod::Spec.new do |s|
  s.name         = "RNSecureKeychain"
  s.version      = "1.0.0"
  s.summary      = "RNSecureKeychain"
  s.description  = <<-DESC
                  RNSecureKeychain
                   DESC
  s.homepage     = "polomcak.com"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "Alex Polomcak" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/Lipo11/react-native-secure-keychain.git", :tag => "master" }
  s.source_files  = "*.{h,m}"

  s.dependency "React"

end

  