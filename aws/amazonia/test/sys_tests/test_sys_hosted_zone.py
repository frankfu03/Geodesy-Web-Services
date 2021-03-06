#!/usr/bin/python3

from amazonia.classes.hosted_zone import HostedZone
from troposphere import ec2, Ref, Tags, Template
from troposphere import elasticloadbalancing as elb


def main():
    template = Template()
    vpc = template.add_resource(ec2.VPC('MyVPC',
                                        CidrBlock='10.0.0.0/16',
                                        EnableDnsSupport='true',
                                        EnableDnsHostnames='true'))

    internet_gateway = template.add_resource(ec2.InternetGateway('MyInternetGateway',
                                                                 Tags=Tags(Name='MyInternetGateway')))

    template.add_resource(ec2.VPCGatewayAttachment('MyVPCGatewayAttachment',
                                                   InternetGatewayId=Ref(internet_gateway),
                                                   VpcId=Ref(vpc),
                                                   DependsOn=internet_gateway.title))

    security_group = template.add_resource(ec2.SecurityGroup(
        'mySecGroup',
        GroupDescription='Security group',
        VpcId=Ref(vpc),
    ))

    template.add_resource(ec2.SecurityGroupEgress(
        'mySecGroupOut',
        CidrIp='0.0.0.0/0',
        IpProtocol='tcp',
        FromPort='0',
        ToPort='0',
        GroupId=Ref(security_group)
    ))

    template.add_resource(ec2.SecurityGroupIngress(
        'mySecGroupIn',
        CidrIp='0.0.0.0/0',
        IpProtocol='tcp',
        FromPort='0',
        ToPort='0',
        GroupId=Ref(security_group)
    ))

    public_subnets = [template.add_resource(ec2.Subnet('MyPubSub1',
                                                       AvailabilityZone='ap-southeast-2a',
                                                       VpcId=Ref(vpc),
                                                       CidrBlock='10.0.1.0/24')),
                      template.add_resource(ec2.Subnet('MyPubSub2',
                                                       AvailabilityZone='ap-southeast-2b',
                                                       VpcId=Ref(vpc),
                                                       CidrBlock='10.0.2.0/24')),
                      template.add_resource(ec2.Subnet('MyPubSub3',
                                                       AvailabilityZone='ap-southeast-2c',
                                                       VpcId=Ref(vpc),
                                                       CidrBlock='10.0.3.0/24'))]

    template.add_resource(elb.LoadBalancer('myLoadBalancer',
                                           CrossZone=True,
                                           HealthCheck=elb.HealthCheck(Target='HTTP:80/index.html',
                                                                       HealthyThreshold='10',
                                                                       UnhealthyThreshold='2',
                                                                       Interval='300',
                                                                       Timeout='60'),
                                           Listeners=[elb.Listener(LoadBalancerPort='80',
                                                                   Protocol='HTTP',
                                                                   InstancePort='80',
                                                                   InstanceProtocol='HTTP')],
                                           Scheme='internet-facing',
                                           SecurityGroups=[Ref(security_group)],
                                           Subnets=[Ref(subnet) for subnet in public_subnets]
                                           ))

    template.add_resource(ec2.Instance(
        'myinstance',
        KeyName='INSERT_YOUR_KEYPAIR_HERE',
        ImageId='ami-dc361ebf',
        InstanceType='t2.nano',
        NetworkInterfaces=[ec2.NetworkInterfaceProperty(
            GroupSet=[Ref(security_group)],
            AssociatePublicIpAddress=True,
            DeviceIndex='0',
            DeleteOnTermination=True,
            SubnetId=Ref(public_subnets[0]))],
        SourceDestCheck=True,
        DependsOn=internet_gateway.title
    ))

    HostedZone(template=template, domain='mypublic.hz', vpcs=None)

    HostedZone(template=template, domain='myprivate.hz', vpcs=[Ref(vpc)])

    print(template.to_json(indent=2, separators=(',', ': ')))


if __name__ == '__main__':
    main()
