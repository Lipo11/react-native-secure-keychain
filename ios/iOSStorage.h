#import <UIKit/UIKit.h>

@interface iOSStorage : NSObject

+ (BOOL)exists:(NSString *)path;
+ (BOOL)write:(NSString *)data to:(NSString *)path;
+ (NSString *)read:(NSString *)path;
+ (BOOL)remove:(NSString *)path;
+ (BOOL)removeAll;

@end
